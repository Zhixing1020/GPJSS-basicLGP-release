package zhixing.symbolicregression.optimization;

import org.spiderland.Psh.intStack;

import ec.EvolutionState;
import ec.Individual;
import ec.app.majority.func.X;
import ec.app.tutorial4.DoubleData;
import ec.app.tutorial4.MultiValuedRegression;
import ec.app.tutorial4.Pow;
import ec.gp.koza.KozaFitness;
import ec.util.Parameter;
//import javafx.scene.chart.PieChart.Data;
import zhixing.jss.cpxInd.individual.GPTreeStruct;
import zhixing.jss.cpxInd.individual.LGPIndividual;
import zhixing.jss.cpxInd.individual.primitive.WriteRegisterGPNode;

public abstract class LGPSymbolicRegression extends MultiValuedRegression {
	public static double data [][];
	public double X[];
	
	//take distance problem as an example
	@Override
	public void setup(final EvolutionState state, final Parameter base) {
		// TODO Auto-generated method stub
		super.setup(state, base);

	}
	
	@Override
	abstract public void evaluate(final EvolutionState state, 
	        final Individual ind, 
	        final int subpopulation,
	        final int threadnum);
}
