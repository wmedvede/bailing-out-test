package org.acme;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kie.kogito.taskassigning.core.model.Group;
import org.kie.kogito.taskassigning.core.model.Task;
import org.kie.kogito.taskassigning.core.model.TaskAssigningSolution;
import org.kie.kogito.taskassigning.core.model.TaskAssignment;
import org.kie.kogito.taskassigning.core.model.User;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;

import static org.kie.kogito.taskassigning.core.model.ModelConstants.DUMMY_TASK_ASSIGNMENT;
import static org.kie.kogito.taskassigning.core.model.ModelConstants.PLANNING_USER;

@QuarkusTest
public class SolverTest {

    private static final String HR = "HR";

    @Inject
    SolverFactory<TaskAssigningSolution> solverFactory;

    /**
     * This example reproduces the Bailing out issue for the task assigning chained graph structure.
     *
     * 1) The following initial solution is created:
     *
     *  User1 -> TaskAssignment1(pinned) -> TaskAssignment2(pinned)
     *
     *  PLANNING_USER (is added with no assignments)
     *
     *  DUMMY_TASK (is added free)
     *
     * 2) Solver is started with the solution in 1)
     *
     * 3) A solution is produced with the following structure:
     *
     * User1 -> TaskAssignment1(pinned) -> TaskAssignment2(pinned)   (no changes in this chain as expected)
     *
     * PLANNING_USER -> DUMMY_TASK (DUMMY_TASK goes to the PLANNING_USER as expected)
     *
     * 4) the solver starts printing the bailing out messages until the LocalSearchPhase finalization:
     *
     * 2021-03-18 10:37:23,222 INFO  [org.opt.cor.imp.sol.DefaultSolver] (main) Solving started: time spent (147), best score (-1init/[0/0]hard/[0/0/0/-4/0/0]soft), environment mode (REPRODUCIBLE), random (JDK with seed 0).
     * New solution arrived!
     * 2021-03-18 10:38:22,274 INFO  [org.opt.cor.imp.con.DefaultConstructionHeuristicPhase] (main) Construction Heuristic phase (0) ended: time spent (59198), best score ([0/0]hard/[-1/-1/0/-5/0/0]soft), score calculation speed (0/sec), step total (1).
     * 2021-03-18 10:38:23,291 WARN  [org.opt.cor.imp.heu.sel.ent.dec.FilteringEntitySelector] (main) Bailing out of neverEnding selector (Filtering(FromSolutionEntitySelector(TaskAssignment))) to avoid infinite loop.
     * 2021-03-18 10:38:23,696 WARN  [org.opt.cor.imp.heu.sel.ent.dec.FilteringEntitySelector] (main) Bailing out of neverEnding selector (Filtering(FromSolutionEntitySelector(TaskAssignment))) to avoid infinite loop.
     * 2021-03-18 10:38:23,792 WARN  [org.opt.cor.imp.heu.sel.ent.dec.FilteringEntitySelector] (main) Bailing out of neverEnding selector (Filtering(FromSolutionEntitySelector(TaskAssignment))) to avoid infinite loop.
     * 2021-03-18 10:38:23,835 WARN  [org.opt.cor.imp.heu.sel.ent.dec.FilteringEntitySelector] (main) Bailing out of neverEnding selector (Filtering(FromSolutionEntitySelector(TaskAssignment))) to avoid infinite loop.
     * 2021-03-18 10:38:23,937 WARN  [org.opt.cor.imp.heu.sel.ent.dec.FilteringEntitySelector] (main) Bailing out of neverEnding selector (Filtering(FromSolutionEntitySelector(TaskAssignment))) to avoid infinite loop.
     * 2021-03-18 10:38:24,091 WARN  [org.opt.cor.imp.heu.sel.ent.dec.FilteringEntitySelector] (main) Bailing out of neverEnding selector (Filtering(FromSolutionEntitySelector(TaskAssignment))) to avoid infinite loop.
     * 2021-03-18 10:38:24,143 WARN  [org.opt.cor.imp.heu.sel.ent.dec.FilteringEntitySelector] (main) Bailing out of neverEnding selector (Filtering(FromSolutionEntitySelector(TaskAssignment))) to avoid infinite loop.
     * 2021-03-18 10:38:25,484 WARN  [org.opt.cor.imp.heu.sel.ent.dec.FilteringEntitySelector] (main) Bailing out of neverEnding selector (Filtering(FromSolutionEntitySelector(TaskAssignment))) to avoid infinite loop.
     * 2021-03-18 10:38:25,567 WARN  [org.opt.cor.imp.heu.sel.ent.dec.FilteringEntitySelector] (main) Bailing out of neverEnding selector (Filtering(FromSolutionEntitySelector(TaskAssignment))) to avoid infinite loop.
     * 2021-03-18 10:38:25,645 WARN  [org.opt.cor.imp.heu.sel.ent.dec.FilteringEntitySelector] (main) Bailing out of neverEnding selector (Filtering(FromSolutionEntitySelector(TaskAssignment))) to avoid infinite loop.
     * 2021-03-18 10:38:25,849 WARN  [org.opt.cor.imp.heu.sel.ent.dec.FilteringEntitySelector] (main) Bailing out of neverEnding selector (Filtering(FromSolutionEntitySelector(TaskAssignment))) to avoid infinite loop.
     * 2021-03-18 10:38:26,279 INFO  [org.opt.cor.imp.loc.DefaultLocalSearchPhase] (main) Local Search phase (1) ended: time spent (63204), best score ([0/0]hard/[-1/-1/0/-5/0/0]soft), score calculation speed (44516/sec), step total (1).
     *
     */
    @Test
    void creatSolver() {
        Task task1 = Task.newBuilder()
                .id("Task1")
                .potentialGroups(Collections.singleton(HR))
                .build();

        Task task2 = Task.newBuilder()
                .id("Task2")
                .potentialGroups(Collections.singleton(HR))
                .build();

        TaskAssignment taskAssignment1 = new TaskAssignment(task1);
        taskAssignment1.setPinned(true);

        TaskAssignment taskAssignment2 = new TaskAssignment(task2);
        taskAssignment2.setPinned(true);

        User user1 = new User("User1", true);
        user1.getGroups().add(new Group(HR));
        user1.setNextElement(taskAssignment1);

        taskAssignment1.setUser(user1);
        taskAssignment1.setPreviousElement(user1);
        taskAssignment1.setNextElement(taskAssignment2);
        taskAssignment1.setStartTimeInMinutes(0);
        taskAssignment1.setEndTimeInMinutes(1);

        taskAssignment2.setUser(user1);
        taskAssignment2.setPreviousElement(taskAssignment1);
        taskAssignment2.setStartTimeInMinutes(1);
        taskAssignment2.setEndTimeInMinutes(2);

        solverFactory.buildSolver();

        TaskAssigningSolution solution = new TaskAssigningSolution("1",
                                                                   Arrays.asList(PLANNING_USER, user1),
                                                                   Arrays.asList(taskAssignment1, taskAssignment2, DUMMY_TASK_ASSIGNMENT));

        Solver<TaskAssigningSolution> solver = solverFactory.buildSolver();

        solver.addEventListener(event -> {
            System.out.println("New solution arrived!");
            /*
                Solution produced by the CH is ok
                    User1 -> TaskAssignment1(pinned) -> TaskAssignment2(pinned)   (no changes in this chain as expected)
                    PLANNING_USER -> DUMMY_TASK                                   (DUMMY_TASK goes to the PLANNING_USER as expected)

                After this initial solution we can see the bailing out messages.
             */

        });

        solver.solve(solution);
    }

}
