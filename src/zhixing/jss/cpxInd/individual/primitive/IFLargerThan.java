package zhixing.jss.cpxInd.individual.primitive;

import ec.EvolutionState;
import ec.Problem;
//import ec.app.tutorial4.DoubleData;
import yimei.jss.gp.data.DoubleData;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;

public class IFLargerThan extends Branching
{
	@Override
	public String toString() { return "IF>#"+this.bodyLength; }

	/*
	public void checkConstraints(final EvolutionState state,
	final int tree,
	final GPIndividual typicalIndividual,
	final Parameter individualBase)
	{
	super.checkConstraints(state,tree,typicalIndividual,individualBase);
	if (children.length!=2)
	state.output.error("Incorrect number of children for node " + 
	toStringForError() + " at " +
	individualBase);
	}
	*/
	@Override
	public int expectedChildren() { return 2; }
	
	@Override
	public void eval(final EvolutionState state,
	    final int thread,
	    final GPData input,
	    final ADFStack stack,
	    final GPIndividual individual,
	    final Problem problem)
	    {
	    double result;
	    DoubleData rd = ((DoubleData)(input));
	
	    children[0].eval(state,thread,input,stack,individual,problem);
	    result = rd.value;
	
	    children[1].eval(state,thread,input,stack,individual,problem);
	    
	    if(result > rd.value){
	    	rd.value = 1.0;
	    }
	    else{
	    	rd.value = 0.0;
	    }
	    
	    }
}
