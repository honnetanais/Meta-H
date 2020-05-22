package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.List;

public class TabooSolver implements Solver {

    /**
     * A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     * <p>
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     * <p>
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     */
    static class Block {
        /**
         * machine on which the block is identified
         */
        final int machine;
        /**
         * index of the first task of the block
         */
        final int firstTask;
        /**
         * index of the last task of the block
         */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }

        public String toString(){ //pour tester
            return "m = " + machine + " 1st = " + firstTask + " Last = " + lastTask;
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     * <p>
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     * <p>
     * The swam with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        public String toString(){ //Pour tester
            return "m = " + machine + " First = " + t1 + " Last = " + t2;
        }

        /**
         * Apply this swap on the given resource order, transforming it into a new solution.
         */
        public void applyOn(ResourceOrder order) {
            Task memoire = order.tasksByMachine[this.machine][this.t1];
            order.tasksByMachine[this.machine][this.t1] = order.tasksByMachine[this.machine][this.t2];
            order.tasksByMachine[this.machine][this.t2] = memoire;
            //System.out.println("Swap applied" + order.toString()); //Pour tester
        }
    }





    @Override
    public Result solve(Instance instance, long deadline) {

        ResourceOrder s = solu(instance).copy(); //s_init
        int[][] sTaboo = new int[instance.numTasks*instance.numJobs][instance.numTasks*instance.numJobs];
        ResourceOrder s_etoile = s.copy();

        int time_out = 0;
        int dureeTaboo = 10;
        int maxIter = 100;
        int k = 0;

        while (k<maxIter && !(time_out == 100)){

            k++;
            //TEST
            //System.out.println("Etape = " + k);

            List<Block> blocks = blocksOfCriticalPath(s,instance);
            ArrayList<ResourceOrder> all_neighbours = new ArrayList<ResourceOrder>();
            ArrayList<Swap> all_swaps = new ArrayList<Swap>();

            //TEST
            /*
            System.out.println("s = ");
            System.out.println(s.toString());
            System.out.println("Critical Path =  ");
            System.out.println(s.toSchedule().criticalPath());
            System.out.println(" sTaboo = ");
            for(int i = 0 ; i < sTaboo.length; i++ ){
                for(int j = 0; j< sTaboo[i].length; j++){

                    System.out.print(" "+sTaboo[i][j]);
                }
                System.out.print("\n");
            }
            for(int i = 0 ; i < blocks.size(); i++){
               System.out.println(blocks.get(i));
            }*/

            /*Pour calculer la liste de voisin d'un ResourceOrder*/
            for(int i = 0; i < blocks.size(); i++){//pour chaque block
                for (int j = 0 ; j < neighbors(blocks.get(i)).size(); j++){
                    ResourceOrder new_neighbor = s.copy();
                    neighbors(blocks.get(i)).get(j).applyOn(new_neighbor);
                    all_neighbours.add(new_neighbor);
                    all_swaps.add(neighbors(blocks.get(i)).get(j)); //on récupère la liste de swap pour voir si ils ne sont pas Taboo
                }
            }

            //TEST
            /*

            System.out.println("NEIGHBOURS");
            for(int i = 0 ; i < all_neighbours.size(); i++){
                System.out.println(all_neighbours.get(i));
            }
            System.out.println("SWAPS");
            for(int i = 0 ; i < all_swaps.size(); i++){
                System.out.println(all_swaps.get(i));
            }*/

            /*On choisit le meilleur voisin non taboo */
            int min = Integer.MAX_VALUE;
            int min_j = -1;
            for(int j = 0 ; j < all_neighbours.size() ; j++){
                if(all_neighbours.get(j).toSchedule().makespan() < min){
                    if(sTaboo[all_swaps.get(j).t1+all_swaps.get(j).machine*s.tasksByMachine[0].length][all_swaps.get(j).t2+all_swaps.get(j).machine*s.tasksByMachine[0].length] <= k){ //if notTaboo
                        //numero de tâche = t1(ou t2) + num de machine * taille
                        min = all_neighbours.get(j).toSchedule().makespan();
                        min_j = j;
                    }
                }
            }

            if(min_j != -1){ //on ne rentre pas ici si on a pas de voisins
                s = all_neighbours.get(min_j).copy();
                sTaboo[all_swaps.get(min_j).t1+all_swaps.get(min_j).machine*s.tasksByMachine[0].length][all_swaps.get(min_j).t2+all_swaps.get(min_j).machine*s.tasksByMachine[0].length] = k + dureeTaboo;

                if(min <= s_etoile.toSchedule().makespan()){ //si la solution s est meilleur que s_etoile, on met à jour s_etoile
                    s_etoile = s.copy();
                }
            }
            time_out++;
        }
        return new Result(instance, s_etoile.toSchedule(), Result.ExitCause.Blocked);
    }

    /**
     * Returns a list of all blocks of the critical path.
     */
    List<Block> blocksOfCriticalPath(ResourceOrder ro, Instance instance) {

        List<Task> crit = ro.toSchedule().criticalPath(); //Le critical path de notre ResourceOrder solution
        ArrayList<Block> blocks = new ArrayList<Block>(); //Le tableau des différents blocks
        Boolean new_b = false; //Indique si on trouve un nouveau block

        Task previous_task = crit.get(0); //previous_task est la tâche à laquelle on compare la tâche suivante pour savoir si elles sont de la même machine ; ici initialisé comme la première tâche du critical path

        for (int i = 1; i < crit.size(); i++) {
            while (i<crit.size() && (instance.machine(previous_task) == instance.machine(crit.get(i)))){
                new_b = true;
                i++;
            }
            if (new_b) {
                int index1 = 0;
                while((index1 < instance.numJobs) && !(ro.tasksByMachine[instance.machine(previous_task)][index1].equals(previous_task))){ //on cherche le num d'index de previous_task
                    index1++;
                }
                int index2 = 0;
                while((index2 < instance.numJobs) && !(ro.tasksByMachine[instance.machine(crit.get(i-1))][index2].equals(crit.get(i-1)))){ //idem pour la tâche finale
                    index2++;
                }
                Block b = new Block(instance.machine(previous_task),index1, index2); //On crée le block en question
                blocks.add(b); // On l'ajoute à notre liste de blocks
                new_b = false; //Plus de nouveau bloc
            }
            if(i<crit.size()) {
                previous_task = crit.get(i); //On initialise notre nouvelle previous_task
            }
        }
        //Pour tester
        /*
        System.out.println(ro);
        System.out.println("Critical path");
        for (int a = 0; a<ro.toSchedule().criticalPath().size();a++){
            System.out.println(ro.toSchedule().criticalPath().get(a) + " sur " + instance.machine(ro.toSchedule().criticalPath().get(a)) );
        }
        for (int a = 0; a<blocks.size();a++){
            System.out.println(blocks.get(a) );
        }*/
        return blocks;
    }

    /**
     * For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood
     */
    List<Swap> neighbors(Block block) {
        ArrayList<Swap> swaps = new ArrayList<Swap>();
        Swap s1 = new Swap(block.machine, block.firstTask, block.firstTask+1);
        swaps.add(s1);
        if (!(block.firstTask == block.lastTask - 1)) { //si il y a plus de 2 tâches dans le block
            Swap s2 = new Swap(block.machine, block.lastTask - 1, block.lastTask);
            swaps.add(s2);
        }
        //Pour tester
        /*
        System.out.println("Block = " + block);
        for(int i = 0 ; i < swaps.size() ; i++){
            System.out.println(" swap " + i + " = "+ swaps.get(i));
        }*/
        return swaps;
    }

    /**
     * Methode EST_LRPT pour la partie gloutonne (simple copier-coller de la partie précédente)
     */
    ResourceOrder solu(Instance instance) {
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
        return sol;
    }
}

