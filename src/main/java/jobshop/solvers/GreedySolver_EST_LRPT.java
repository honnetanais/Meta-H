package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.stream.IntStream;

import static java.lang.System.*;


public class GreedySolver_EST_LRPT implements Solver {

    @Override
    public Result solve(Instance instance, long deadline) {

            ResourceOrder sol = new ResourceOrder(instance);
            ArrayList<Task> realisable = new ArrayList<Task>();

            //Les premières tâches réalisables = les premières tâches de chaque jobs
            for (int j = 0; j < instance.numJobs; j++) {
                realisable.add(j, new Task(j, 0)); //on compte à O
            }

            int[] jobs = new int[instance.numJobs]; //tableau de durée des jobs

            /* On initialise le tableau à la longueur max de chaque job */
            for (int j = 0; j < instance.numJobs; j++) {
                for (int t = 0; t < instance.numTasks; t++) {
                    jobs[j] += instance.duration(j, t); //on parcourt toutes les tâches pour voir le job le plus long
                }
            }

            /*Utilisé pour les algorithmes EST*/
            int[] releaseTimeOfMachine = new int[instance.numMachines]; //Date de libération de la machine
            int[] releaseTimeOfJob = new int[instance.numJobs]; //Date de terminaison du job

            ArrayList<Integer> earliest = new ArrayList<Integer>(); //On met dans cet Array le numero d'index qui correspond à la/les tâche(s) réalisables le plus tôt


            while (!realisable.isEmpty()) { //Tant qu'il reste des tâches réalisables

                int est = Integer.MAX_VALUE;

                for (int i = 0; i < realisable.size(); i++) {
                    int start = Math.max(releaseTimeOfMachine[instance.machine(realisable.get(i))], releaseTimeOfJob[realisable.get(i).job]);
                    if (start < est) {
                        est = start;
                        earliest.clear(); //On enlève les minimums enregistrés précédement
                        earliest.add(i); //On rajoute le numéro de tâche correspondant
                    } else if (start == est) {
                        earliest.add(i);//Si 2 tâches dont de même duree minimum, on garde les 2 en memoires pour leur appliquer ensuite l'algorithme glouton
                    }
                }

                /* Chercher le job restant le plus long */
                int dur = Integer.MIN_VALUE;
                int num_job = -1;
                for (int i = 0; i < earliest.size(); i++) {
                    int prov = jobs[realisable.get(earliest.get(i)).job]; //le temps restant du job corresponsant aux tâches qui commencent le plus töt
                    if (prov > dur) {
                        dur = prov;
                        num_job = realisable.get(earliest.get(i)).job;
                    }
                }

                /* Chercher la tache réalisable correspondant à ce job */
                int num = 0;
                while (!(realisable.get(num).job == num_job) && (num < realisable.size())) { //gérer les parenthèses
                    num++;
                } //que se passe il si jamais aucune case de realisable ne contient ce num_job => il va renvoyer la deriere case de réalisable ==> à tester


                /*Mise à jour du ressourceOrder*/
                sol.tasksByMachine[instance.machine(realisable.get(num))][sol.nextFreeSlot[instance.machine(realisable.get(num))]] = realisable.get(num);
                sol.nextFreeSlot[instance.machine(realisable.get(num))]++; //+1 au nextfreeslot de cette machine

                if ((realisable.get(num).task + 1) < instance.numTasks) { //si il reste des tâches à ajouter
                    Task nouvelle = new Task(realisable.get(num).job, realisable.get(num).task + 1); //On cherche ses predecesseurs
                    realisable.add(nouvelle);
                }

                /*On enlève cette durée du tableau jobs*/
                jobs[num_job] -= instance.duration(realisable.get(num));

                releaseTimeOfMachine[instance.machine(realisable.get(num))] = (est + instance.duration(realisable.get(num))); //On rajoute la durée de la tâche choisie à la machine
                releaseTimeOfJob[realisable.get(num).job] = (est + instance.duration(realisable.get(num))); //On rajoute la durée de la tâche choisie au job

                /*On la supprime des tâches réalisables*/
                realisable.remove(num);

            }
        //System.out.println(sol.toString());
        return new Result(instance, sol.toSchedule(), Result.ExitCause.Blocked);
    }



}

