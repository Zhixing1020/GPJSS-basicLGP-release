package zhixing.jss.cpxInd.simulation.events;

import yimei.jss.jobshop.Job;
import yimei.jss.simulation.DecisionSituation;
import zhixing.jss.cpxInd.simulation.events.OperationVisitEvent4Ind;

import java.util.List;

import ec.EvolutionState;
import zhixing.jss.cpxInd.simulation.DynamicSimulation4Ind;
import zhixing.jss.cpxInd.simulation.Simulation4Ind;
import zhixing.jss.cpxInd.simulation.events.AbstractEvent4Ind;

public class JobArrivalEvent4Ind extends AbstractEvent4Ind {

    private Job job;

    public JobArrivalEvent4Ind(double time, Job job) {
        super(time);
        this.job = job;
    }

    public JobArrivalEvent4Ind(Job job) {
        this(job.getArrivalTime(), job);
    }

    @Override
    public void trigger(Simulation4Ind simulation) {
        job.getOperation(0).setReadyTime(job.getReleaseTime());

        simulation.addEvent(
                new OperationVisitEvent4Ind(job.getReleaseTime(), job.getOperation(0)));

        simulation.generateJob();
    }
    

//    public void trigger(Simulation simulation, EvolutionState state, int col) {
//        job.getOperation(0).setReadyTime(job.getReleaseTime());
//
//        simulation.addEvent(
//                new OperationVisitEvent(job.getReleaseTime(), job.getOperation(0)));
//
//        simulation.generateJob();
//    }

    @Override
    public void addDecisionSituation(DynamicSimulation4Ind simulation,
                                     List<DecisionSituation> situations,
                                     int minQueueLength) {
        trigger(simulation);
    }

    @Override
    public String toString() {
        return String.format("%.1f: job %d arrives.\n", time, job.getId());
    }

    @Override
    public int compareTo(AbstractEvent4Ind other) {
        if (time < other.getTime())
            return -1;

        if (time > other.getTime())
            return 1;

        if (other instanceof JobArrivalEvent4Ind)
            return 0;

        return -1;
    }
}
