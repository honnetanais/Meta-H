package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.Encoding;

import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.Optional;
import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.stream.IntStream;

import static java.lang.System.*;


public class GreedySolver_SPT implements Solver {

    @Override
    public Result solve(Instance instance, long deadline) {
        ResourceOrder sol = new ResourceOrder(instance);
        ArrayList<Task> realisable = new ArrayList<Task>();

        /*
        int tour = 0;
        //Pour afficher le ArrayList realisable à chaque tour, m'a servi pour debugguer et verifier mes résultats
        tour++;
        System.out.print("Tour = " + tour + " \n");
        for (int i = 0; i < realisable.size(); i++) {
            System.out.printf(realisable.get(i).toString() + " \n");
        }
        */

        //Les premières tâches réalisables = les premières tâches de chaque jobs
        for(int j = 0 ; j<instance.numJobs ; j++) {
            realisable.add(j,new Task(j,0)); //on compte à O
        }

        while(!realisable.isEmpty()) { //Tant qu'il reste des tâches réalisables

                /*Choisir une des taches a mettre dans le ressourceOrder*/
                int dur = Integer.MAX_VALUE;
                int num = -1;
                for (int i = 0; i < realisable.size(); i++) {
                    int prov = instance.duration(realisable.get(i));
                    if (prov < dur) {
                        dur = prov;
                        num = i;
                    }
                }
                /*Mise à jour du ressourceOrder*/
                sol.tasksByMachine[instance.machine(realisable.get(num))][sol.nextFreeSlot[instance.machine(realisable.get(num))]] = realisable.get(num);
                sol.nextFreeSlot[instance.machine(realisable.get(num))]++; //+1 au nextfreeslot de cette machine

                if ((realisable.get(num).task + 1) < instance.numTasks) { //si il reste des tâches à ajouter
                    Task nouvelle = new Task(realisable.get(num).job, realisable.get(num).task + 1); //On cherche ses predecesseurs
                    realisable.add(nouvelle);
                }
                realisable.remove(num); //On la supprime des tâches réalisables
            }

        //System.out.println(sol.toString());

        return new Result(instance, sol.toSchedule(), Result.ExitCause.Blocked);
    }
}

