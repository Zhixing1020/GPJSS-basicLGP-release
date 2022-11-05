package zhixing.jss.cpxInd.individual;

import java.util.Stack;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;

public abstract class FlowController {
	public Stack<Integer> beginIndex;
	public Stack<Integer> endIndex; //beginIndex + bodyLength
	public int currentIndex;
	
	public int maxIterTimes;
	public int currentIterTimes;
	
	public FlowController(){
		beginIndex = new Stack<Integer>();
		endIndex = new Stack<Integer>();
	}
	
	public void resetFlowController(int maxinstr){
		beginIndex = new Stack<Integer>();
		endIndex = new Stack<Integer>();
		
		currentIndex = 0;
		currentIterTimes = 0;
		
		beginIndex.push(currentIndex);
		endIndex.push(maxinstr-1); // means the last index of a program
	}
	
	public abstract void execute(EvolutionState state, int thread, GPData input, ADFStack stack, CpxGPIndividual individual, Problem problem);
}
