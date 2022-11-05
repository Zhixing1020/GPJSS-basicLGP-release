package zhixing.jss.cpxInd.individual;

import java.util.ArrayList;
import java.util.List;

import ec.EvolutionState;
import ec.Problem;
//import ec.app.tutorial4.DoubleData;
import yimei.jss.gp.data.DoubleData;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPNode;
import zhixing.jss.cpxInd.individual.primitive.FlowOperator;

public class LGPFlowController extends FlowController {
	private ArrayList<GPTreeStruct> trees;
	
	public LGPFlowController(){
		super();
	}
	
	public void execute(EvolutionState state, int thread, GPData input, ADFStack stack, CpxGPIndividual individual, Problem problem){
		//trees = new ArrayList<>(((LGPIndividual)individual).getTreeStructs());
		trees = (ArrayList<GPTreeStruct>) ((LGPIndividual)individual).getTreeStructs();
		
		resetFlowController(trees.size());
		
		executeProgramBlock(null, 0, input, null, individual, problem);
	}
	
	private void executeProgramBlock(EvolutionState state, int thread, GPData input, ADFStack stack, CpxGPIndividual individual, Problem problem){
		//get the instruction of currentIndex, if arithmetic instruction, evaluate and increase currentIndex
		//if flow control instruction, put currentIndex as beginIndex and beginIndex + bodyLength as endIndex
		GPTreeStruct instr;
		do{
			instr = trees.get(currentIndex);
			GPNode newroot;
			switch(instr.type){
			case 0:  //arithmetic
				if(instr.status) instr.child.eval(state, thread, input, stack, individual, problem);
				currentIndex ++;
				break;
			case 1: //branching
				newroot = instr.child.children[0]; //got the flow control operator
				newroot.eval(state, thread, input, stack, individual, problem);
				//if(!instr.status || ((DoubleData)input).x == 0.0){
				if(!instr.status || ((DoubleData)input).value == 0.0){
					currentIndex += ((FlowOperator)newroot).getBodyLength() + 1;
				}
				else{
					beginIndex.push(currentIndex);
					endIndex.push(currentIndex + ((FlowOperator)newroot).getBodyLength());
					currentIndex ++;
					executeProgramBlock(state, thread, input, stack, individual, problem);
				}
				break;
			case 2: //iteration
				newroot = instr.child.children[0]; //got the flow control operator
				newroot.eval(state, thread, input, stack, individual, problem);
				//if(((DoubleData)input).x == 0.0){
				if(((DoubleData)input).value == 0.0){
					currentIndex += ((FlowOperator)newroot).getBodyLength() + 1;
				}
				else if(currentIterTimes >= maxIterTimes || ! instr.status){ 
					//an iteration will be effective only when it 1) contains effective instructions 
					//and 2) contains an effective register which is simultaneously source and destination register
					currentIndex++;
				}
				else{
					beginIndex.push(currentIndex);
					endIndex.push(currentIndex + ((FlowOperator)newroot).getBodyLength());
					currentIndex ++;
					executeProgramBlock(state, thread, input, stack, individual, problem);
				}
				break;
			default:
				state.output.fatal("we got an illegal instruction type in LGPFlowController\n");
				System.exit(1);
			}
			
			
		}while(currentIndex < trees.size() && currentIndex <= endIndex.peek());
		
		if(trees.get(beginIndex.peek()).type == 2 && currentIterTimes < maxIterTimes){
			currentIterTimes ++;
			currentIndex = beginIndex.peek(); //back to the beginning place of the iteration
		}
		
		beginIndex.pop();
		endIndex.pop();
	}

}
