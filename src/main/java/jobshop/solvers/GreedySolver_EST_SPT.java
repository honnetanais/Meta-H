package jobshop.solvers;

import jobshop.*;

import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.System.*;


public class GreedySolver_EST_SPT implements Solver {

    @Override
    public Result solve(Instance instance, long deadline) {

        ResourceOrder sol = new ResourceOrder(instance);
        ArrayList<Task> realisable = new ArrayList<Task>();

        //Les premières tâches réalisables = les premières tâches de chaque jobs
        for(int j = 0 ; j<instance.numJobs ; j++) {
            realisable.add(new Task(j,0)); //on compte à O
        }

        /*Utilisé pour les algorithmes EST*/
        int[] releaseTimeOfMachine = new int[instance.numMachines]; //Date de libération de la machine
        int[] releaseTimeOfJob = new int[instance.numJobs]; //Date de libération du job

        ArrayList<Integer> earliest =  new ArrayList<Integer>(); //On met dans cet Array le numero d'index qui correspond à la/les tâche(s) réalisables le plus tôt

        while(!realisable.isEmpty()) {

            int est = Integer.MAX_VALUE;

            for (int i = 0; i < realisable.size(); i++) {
                int start = Math.max(releaseTimeOfMachine[instance.machine(realisable.get(i))], releaseTimeOfJob[realisable.get(i).job]);
                if (start < est) {
                    est = start;
                    earliest.clear(); //On enlève les minimums enregistrés précédement
                    earliest.add(i); //On rajoute le numéro de tâche correspondant
                }
                else if (start == est) {
                    earliest.add(i);//Si 2 tâches dont de même duree minimum, on garde les 2 en memoires pour leur appliquer ensuite l'algorithme glouton
                }
            }

            int dur = Integer.MAX_VALUE;
            int num = -1;
            for (int i = 0; i < earliest.size(); i++) { //Si il n'y a qu'une seule tâche dans en attente, on ne fait qu'un tour qui permet d'initialiser num
                int prov = instance.duration(realisable.get(earliest.get(i)));
                if (prov < dur) {
                    dur = prov;
                    num = (earliest.get(i));
                }
            }

            /*Mise à jour du ressourceOrder*/
            sol.tasksByMachine[instance.machine(realisable.get(num))][sol.nextFreeSlot[instance.machine(realisable.get(num))]] = realisable.get(num);
            sol.nextFreeSlot[instance.machine(realisable.get(num))]++; //+1 au nextfreeslot de cette machine

            if ((realisable.get(num).task + 1) < instance.numTasks) { //si il reste des tâches à ajouter
                Task nouvelle = new Task(realisable.get(num).job, realisable.get(num).task + 1); //On cherche ses predecesseurs
                realisable.add(nouvelle);
            }


            releaseTimeOfMachine[instance.machine(realisable.get(num))] = (est + instance.duration(realisable.get(num))); //On rajoute la durée de la tâche choisie à la machine
            releaseTimeOfJob[realisable.get(num).job] =(est + instance.duration(realisable.get(num))); //On rajoute la durée de la tâche choisie au job

            realisable.remove(num); //On la supprime des tâches réalisables
        }
    //System.out.println(sol.toString());

        return new Result(instance, sol.toSchedule(), Result.ExitCause.Blocked);
}
}

