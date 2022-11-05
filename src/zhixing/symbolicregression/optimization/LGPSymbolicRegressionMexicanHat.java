package zhixing.symbolicregression.optimization;

import org.apache.commons.math3.random.ISAACRandom;
import org.spiderland.Psh.intStack;

//import com.sun.istack.internal.FinalArrayList;

import ec.EvolutionState;
import ec.Individual;
import ec.app.tutorial4.DoubleData;
import ec.app.tutorial4.MultiValuedRegression;
import ec.gp.GPIndividual;
import ec.gp.koza.KozaFitness;
import ec.util.Parameter;
import zhixing.jss.cpxInd.individual.GPTreeStruct;
import zhixing.jss.cpxInd.individual.LGPIndividual;
import zhixing.jss.cpxInd.individual.primitive.WriteRegisterGPNode;

public class LGPSymbolicRegressionMexicanHat extends LGPSymbolicRegression{
	
	@Override
	public void setup(final EvolutionState state, final Parameter base) {
		// TODO Auto-generated method stub
		super.setup(state, base);
		
		if(data==null) {
			data = new double[400][2];
			
			for(int y = 0; y < 20; y++) {
				currentY = -3.8 + 0.4*y;
				for(int x = 0; x < 20; x++) {
					currentX = -3.8 + 0.4*x;
					data[x + 20*y][0] = currentX;
		            data[x + 20*y][1] = currentY;
				}
//				currentX = state.random[0].nextDouble()*8.0-4;
//	            currentY = state.random[0].nextDouble()*8.0-4;
				
			}
		}
		
		X = new double [2];
	}
	
	@Override
	public void evaluate(final EvolutionState state, 
	        final Individual ind, 
	        final int subpopulation,
	        final int threadnum)
	        {
	        if (!ind.evaluated)  // don't bother reevaluating
	            {
	            DoubleData input = (DoubleData)(this.input);
	        
	            int hits = 0;
	            double sum = 0.0;
	            double expectedResult;
	            double result;
	            for (int y=0;y<400;y++)
	                {
	                //currentX = state.random[threadnum].nextDouble()*8.0-4;
	                //currentY = state.random[threadnum].nextDouble()*8.0-4;
	            	X[0] = data[y][0];
	            	X[1] = data[y][1];
	                //expectedResult = currentX*currentX*currentY + currentX*currentY + currentY;
	                expectedResult = (1-X[0]*X[0]/4-X[1]*X[1]/4)*Math.exp(-X[0]*X[0]/8-X[1]*X[1]/8);
	                DoubleData tmp = new DoubleData();
//	                int treeNum = ((LGPIndividual)ind).getTreesLength();
//	                
//	                ((LGPIndividual)ind).resetRegisters(this);
//	                
//	        		for(int index = 0; index<treeNum; index++){
//	        			GPTreeStruct tree = ((LGPIndividual)ind).getTreeStruct(index);
//	        			if(tree.status) {
//	        				tree.child.eval(null, 0, tmp, null, (LGPIndividual)ind, this);
//		        			 if(((LGPIndividual)ind).getRegisters()[((WriteRegisterGPNode)tree.child).getIndex()] 
//		        					 >= Double.POSITIVE_INFINITY) {
//		 	                	int a =1;
//		 	                }
//	        			}
//	        			
//	        		}
	                ((LGPIndividual)ind).execute(null, 0, tmp, null, (LGPIndividual)ind, this);

	                result = Math.abs(expectedResult - ((LGPIndividual)ind).getRegisters()[0]);
	                
	                if(result >= Double.POSITIVE_INFINITY || Double.isNaN(result)) {
	                	result = 1e6;
	                }
	               
	                if (result <= 0.01) hits++;
	                sum += result*result;                  
	                }

	            // the fitness better be KozaFitness!
	            KozaFitness f = ((KozaFitness)ind.fitness);
	            f.setStandardizedFitness(state, sum);
	            f.hits = hits;
	            ind.evaluated = true;
	            }
	        }
}
