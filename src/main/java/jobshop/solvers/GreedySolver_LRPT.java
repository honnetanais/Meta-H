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


public class GreedySolver_LRPT implements Solver {

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

        /*Les premières tâches réalisables = les premières tâches de chaque jobs*/
        for(int j = 0 ; j<instance.numJobs ; j++) {
            realisable.add(j,new Task(j,0)); //on compte à O
        }

        int[] jobs = new int[instance.numJobs]; //Tableau de durée des jobs

        /*for (int i = 0; i<instance.numJobs ; i++) {
            jobs[i] = 0; //initilisation du tableau à 0
        }*/ //java initialise un tableau d'entier à 0 par défaut

        /* On initialise le tableau à la longueur max de chaque job */
        for (int j = 0; j < instance.numJobs; j++) {
            for (int t = 0; t < instance.numTasks; t++) {
                jobs[j] += instance.duration(j,t); //on parcourt toutes les tâches pour voir le job le plus long
            }
        }

        while(!realisable.isEmpty()) { //Tant qu'il reste des tâches réalisables

            /* Chercher le job restant le plus long */
            int dur = Integer.MIN_VALUE;
            int num_job = -1;
            for(int i = 0 ; i<jobs.length ; i++){
                int prov = jobs[i];
                if (prov > dur) {
                    dur = prov;
                    num_job = i;
                }
            }

            /* Chercher la tache réalisable correspondant à ce job */
            int num = 0;
            while(!(realisable.get(num).job == num_job) && (num < realisable.size())){
                num++;
            }


            /*Mise à jour du ressourceOrder*/
            sol.tasksByMachine[instance.machine(realisable.get(num))][sol.nextFreeSlot[instance.machine(realisable.get(num))]] = realisable.get(num);
            sol.nextFreeSlot[instance.machine(realisable.get(num))]++; //+1 au nextfreeslot de cette machine

            if ((realisable.get(num).task + 1) < instance.numTasks) { //si il reste des tâches à ajouter
                Task nouvelle = new Task(realisable.get(num).job, realisable.get(num).task + 1); //On cherche ses predecesseurs
                realisable.add(nouvelle);
            }

            /*On enlève cette durée du tableau jobs*/
            jobs[num_job] -= instance.duration(realisable.get(num));

            /*On la supprime des tâches réalisables*/
            realisable.remove(num);

        }
        //System.out.println(sol.toString());
        return new Result(instance, sol.toSchedule(), Result.ExitCause.Blocked);
    }



}

