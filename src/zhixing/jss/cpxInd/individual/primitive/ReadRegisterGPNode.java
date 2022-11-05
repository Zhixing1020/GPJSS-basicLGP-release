package zhixing.jss.cpxInd.individual.primitive;

import org.spiderland.Psh.intStack;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Parameter;
import yimei.jss.gp.CalcPriorityProblem;
import yimei.jss.gp.data.DoubleData;
//import ec.app.tutorial4.DoubleData;
import yimei.jss.gp.terminal.AttributeGPNode;
import yimei.jss.gp.terminal.JobShopAttribute;
import zhixing.jss.cpxInd.individual.LGPIndividual;

public class ReadRegisterGPNode extends GPNode {
	
	public final static String P_NUMREGISTERS = "numregisters";
	
	protected int index;
	
	protected static int range;
	
	@Override
	public void setup(final EvolutionState state, final Parameter base){
		super.setup(state, base);
		Parameter pp_numreg = base.push(P_NUMREGISTERS);
    	range = state.parameters.getInt(pp_numreg,null,0);
    	if(range < 1) {
    		state.output.fatal("number of registers must be >=1");
    	}

	}
	
    public ReadRegisterGPNode(int ind, int range) {
        super();
        children = new GPNode[0];
        this.index = ind;
        
        this.range = range;
    }
    
    public ReadRegisterGPNode(int ind) {
        super();
        children = new GPNode[0];
        this.index = ind;
    }
    
    public ReadRegisterGPNode() {
        super();
        children = new GPNode[0];
        this.index = 0;
    }

    public int getIndex() {
        return index;
    }
    
    public void setIndex(int ind){
    	index = ind;
    	
    }

    @Override
    public String toString() {
        return "R" + index;
    }

    @Override
    public int expectedChildren() {
        return 0;
    }

    @Override
    public void eval(EvolutionState state, int thread, GPData input,
                     ADFStack stack, GPIndividual individual, Problem problem) {

        DoubleData data = ((DoubleData)input);
        LGPIndividual ind = ((LGPIndividual)individual);
        data.value = ind.getRegisters(index);
        //data.x = ((LGPIndividual)individual).getRegisters()[index];
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public boolean equals(Object other) {
        if (other instanceof ReadRegisterGPNode) {
        	ReadRegisterGPNode o = (ReadRegisterGPNode)other;
            return (index == o.getIndex());
        }

        return false;
    }
    
    @Override
    public void resetNode(final EvolutionState state, final int thread) {
    	index = state.random[thread].nextInt(range);
    }

}
