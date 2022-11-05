package zhixing.primitives;

import java.util.Vector;

import org.spiderland.Psh.intStack;

import ec.EvolutionState;
import ec.Problem;
import ec.app.tutorial4.DoubleData;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import zhixing.jss.cpxInd.individual.LGPIndividual;
import zhixing.jss.cpxInd.individual.primitive.ReadRegisterGPNode;

public class ConstantGPNode extends GPNode{
	private Double value;
	private static Vector<Double> range;
	
	//ConstantGPNode and InputFeatureGPNode can also develop a setup function like Write(Read)RegisterGPNode, and be used in GPFunctionSet
	
	public ConstantGPNode() {
		super();
		children = new GPNode[0];
		this.value = 1.;
	}
	
	public ConstantGPNode(double val) {
		super();
		children = new GPNode[0];
		this.value = val;
	}
	
	public ConstantGPNode(double begin, double end, double step) {
		super();
		children = new GPNode[0];
		range = new Vector<>();
		for(double i = begin; i<=end;i+=step) {
			range.add(i);
		}
		this.value = 1.;
	}
	
	public Double getValue() {
		return value;
	}
	
	public void setValue(Double v) {
		value = v;
	}
	
	@Override
    public String toString() {
        return value.toString();
    }

    @Override
    public int expectedChildren() {
        return 0;
    }

    @Override
    public void eval(EvolutionState state, int thread, GPData input,
                     ADFStack stack, GPIndividual individual, Problem problem) {

        DoubleData data = ((DoubleData)input);
        //data.value = ((LGPIndividual)individual).getRegisters()[index];
        data.x = value;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public boolean equals(Object other) {
        if (other instanceof ConstantGPNode) {
            return value == ((ConstantGPNode) other).getValue();
        }

        return false;
    }
    
    @Override
    public void resetNode(final EvolutionState state, final int thread) {
    	value = range.get(state.random[thread].nextInt(range.size()));
    }
}
