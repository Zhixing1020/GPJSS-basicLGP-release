package zhixing.jss.cpxInd.individual;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.List;

import ec.EvolutionState;
import ec.Fitness;
import ec.Individual;
import ec.gp.GPDefaults;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import yimei.jss.jobshop.Objective;
import yimei.jss.jobshop.Operation;
import yimei.jss.jobshop.SchedulingSet;
import yimei.jss.jobshop.WorkCenter;
import yimei.jss.rule.AbstractRule;
import yimei.jss.simulation.DecisionSituation;
import yimei.jss.simulation.state.SystemState;
import zhixing.jss.cpxInd.jobshop.SchedulingSet4Ind;
import zhixing.jss.cpxInd.simulation.DynamicSimulation4Ind;
import zhixing.jss.cpxInd.simulation.Simulation4Ind;

public abstract class CpxGPIndividual extends GPIndividual{
	//The biggest differences between CpxGPIndividual and GPIndividual are using STL collections to contain GPTrees
	//and these individuals will contain more information.
	
	//To be compatible with existing functions of GPIndividual, CpxGPIndividual inherits GPIndividuals, with a lot of different features
	
	private static final long serialVersionUID = 1;

    public static final String P_NUMTREES = "numtrees";
    public static final String P_TREE = "tree";
    
    public abstract void rebuildIndividual(EvolutionState state, int thread);
	
	public void calcFitnessInd(Fitness fitness, EvolutionState state, SchedulingSet4Ind schedulingSet,
			List<Objective> objectives){
		 
			double[] fitnesses = new double[objectives.size()];

			this.prepareExecution();
			
			schedulingSet.setIndividual(this);
			List<Simulation4Ind> simulations = schedulingSet.getSimulations();
			int col = 0;

			//System.out.println("The simulation size is "+simulations.size()); //1
			for (int j = 0; j < simulations.size(); j++) {
				Simulation4Ind simulation = simulations.get(j);
				//simulation.rerun(this, state, col);
				simulation.rerun();

				for (int i = 0; i < objectives.size(); i++) {
					// System.out.println("Makespan:
					// "+simulation.objectiveValue(objectives.get(i)));
					// System.out.println("Benchmark makespan:
					// "+schedulingSet.getObjectiveLowerBound(i, col));
					double normObjValue = simulation.objectiveValue(objectives.get(i));
					//		/ (schedulingSet.getObjectiveLowerBound(i, col));

					//modified by fzhang, 26.4.2018  check in test process, whether there is ba
					fitnesses[i] += normObjValue;
				}

				col++;

				//System.out.println("The value of replication is "+schedulingSet.getReplications()); //50
				for (int k = 1; k < schedulingSet.getReplications().get(j); k++) {
					//simulation.rerun(this, state, col);
					simulation.rerun();

					for (int i = 0; i < objectives.size(); i++) {
						double normObjValue = simulation.objectiveValue(objectives.get(i));
						//		/ (schedulingSet.getObjectiveLowerBound(i, col)+1e-6);
						fitnesses[i] += normObjValue;
					}

					col++;
				}

				simulation.reset();
			}

			for (int i = 0; i < fitnesses.length; i++) {
				fitnesses[i] /= col;
			}
			MultiObjectiveFitness f = (MultiObjectiveFitness) fitness;
			f.setObjectives(state, fitnesses);
	 }
	
	public void calcFitnessInd4OneTask(Fitness fitness, EvolutionState state, SchedulingSet4Ind schedulingSet,
			List<Objective> objectives, int simIndex){
		double[] fitnesses = new double[objectives.size()];

		this.prepareExecution();
		
		schedulingSet.setIndividual(this);
		Simulation4Ind simulation = schedulingSet.getSimulations().get(simIndex);
		int col = 0;

		//System.out.println("The simulation size is "+simulations.size()); //1
			//Simulation4Ind simulation = simulations.get(j);
			//simulation.rerun(this, state, col);
			simulation.rerun();

			for (int i = 0; i < objectives.size(); i++) {
				// System.out.println("Makespan:
				// "+simulation.objectiveValue(objectives.get(i)));
				// System.out.println("Benchmark makespan:
				// "+schedulingSet.getObjectiveLowerBound(i, col));
				double normObjValue = simulation.objectiveValue(objectives.get(i));
				//		/ (schedulingSet.getObjectiveLowerBound(i, col));

				//modified by fzhang, 26.4.2018  check in test process, whether there is ba
				fitnesses[i] += normObjValue;
			}

			col++;

			//System.out.println("The value of replication is "+schedulingSet.getReplications()); //50
			for (int k = 1; k < schedulingSet.getReplications().get(simIndex); k++) {
				//simulation.rerun(this, state, col);
				simulation.rerun();

				for (int i = 0; i < objectives.size(); i++) {
					double normObjValue = simulation.objectiveValue(objectives.get(i));
					//		/ (schedulingSet.getObjectiveLowerBound(i, col)+1e-6);
					fitnesses[i] += normObjValue;
				}

				col++;
			}

			simulation.reset();
		

		for (int i = 0; i < fitnesses.length; i++) {
			fitnesses[i] /= col;
		}
		
		MultiObjectiveFitness f = (MultiObjectiveFitness) fitness;
		if(f.getNumObjectives()>1){
			f.setObjectives(state, fitnesses);
		}
		else{
			double [] tmp_f = new double [1];
			tmp_f[0] = fitnesses[simIndex];
			f.setObjectives(state, tmp_f);
		}
	}
	
	@Override
	public void setup(final EvolutionState state, final Parameter base){
		super.setup_extensive(state, base);
	}
	
	@Override
	public abstract boolean equals(Object ind);
	
	@Override
	public abstract int hashCode();
	
	public abstract void verify(EvolutionState state);
	
	public abstract void printTrees(final EvolutionState state, final int log);
	
	@Override
	public abstract void printIndividualForHumans(final EvolutionState state, final int log);
	
	@Override
	public abstract void printIndividual(final EvolutionState state, final int log);
	
	@Override
	public abstract void printIndividual(final EvolutionState state, final PrintWriter writer);
	
	/** Overridden for the GPIndividual genotype. */
	@Override
    public abstract void writeGenotype(final EvolutionState state,
        final DataOutput dataOutput)throws IOException;

    /** Overridden for the GPIndividual genotype. */
	@Override
    public abstract void readGenotype(final EvolutionState state,
        final DataInput dataInput)throws IOException;

	@Override
    public abstract void parseGenotype(final EvolutionState state,
        final LineNumberReader reader)throws IOException;
	
	@Override
	public Object clone(){
		return super.clone_extensive();
	}

	/** Like clone(), but doesn't force the GPTrees to deep-clone themselves. */
	public abstract CpxGPIndividual lightClone();
	
	/** Returns the "size" of the individual, namely, the number of nodes
	    in all of its subtrees.  */
	@Override
	public abstract long size();
	
	public Operation priorOperation(DecisionSituation decisionSituation) {
        List<Operation> queue = decisionSituation.getQueue();
        WorkCenter workCenter = decisionSituation.getWorkCenter();
        SystemState systemState = decisionSituation.getSystemState();

        Operation priorOp = queue.get(0);
        priorOp.setPriority(
                priority(priorOp, workCenter, systemState));

        for (int i = 1; i < queue.size(); i++) {
            Operation op = queue.get(i);
            op.setPriority(priority(op, workCenter, systemState));

            if (op.priorTo(priorOp))
                priorOp = op;
        }
        
        

        return priorOp;
    }
	
	public abstract double priority(Operation op,
            WorkCenter workCenter,
            SystemState systemState);
	
	public void prepareExecution(){};
}
