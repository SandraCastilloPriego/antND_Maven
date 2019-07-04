package com.vtt.antnd.modules.simulation.FBA;


import com.vtt.antnd.data.antSimData.ReactionFA;
import com.vtt.antnd.modules.simulation.FBA.LP.ConType;
import com.vtt.antnd.modules.simulation.FBA.LP.ObjType;
import com.vtt.antnd.modules.simulation.FBA.LP.Solver;
import com.vtt.antnd.modules.simulation.FBA.LP.VarType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sbml.jsbml.Model;

public abstract class Analysis {

    protected double maxObj = Double.NaN;
    List< Double> objectiveList;
    private Map<String, Integer> reactionPositionMap;
    private Map<String, Integer> metabolitePositionMap;
    List<ReactionFA> reactionsList;
    List<String> metabolitesList;
    private ObjType direction = ObjType.Maximize;

    protected void setVars() {
        for (ReactionFA r : reactionsList) {
            String varName = Integer.toString(this.reactionPositionMap.get(r.getId()));
            //System.out.println(r.getId() + ": " + varName + ": " + r.getlb() + " ," + r.getub());            
            this.getSolver().setVar(varName, VarType.CONTINUOUS, r.getlb(), r.getub());

        }
    }

    protected void setConstraints() {
        setConstraints(ConType.EQUAL, 0.0);
    }

    protected void setConstraints(ConType conType, double bValue) {

        ArrayList< Map< Integer, Double>> sMatrix = this.getSMatrix();
        for (int i = 0; i < sMatrix.size(); i++) {
            this.getSolver().addConstraint(sMatrix.get(i), conType, bValue);
        }
    }
    
    protected ArrayList< Map< Integer, Double>> getSMatrix() {
        this.metabolitePositionMap = new HashMap<>();
        ArrayList< Map< Integer, Double>> sMatrix = new ArrayList<>(
            this.metabolitesList.size());
        for (int i = 0; i < this.metabolitesList.size(); i++) {
            this.metabolitePositionMap.put(this.metabolitesList.get(i), i);
            Map< Integer, Double> sRow = new HashMap<>();
            sMatrix.add(sRow);
        }

        for (ReactionFA reaction : this.reactionsList) {
            for (String reactant : reaction.getReactants()) {
                if (this.metabolitesList.contains(reactant)) {
                    double sto = reaction.getStoichiometry(reactant);
                    sto = Math.abs(sto) * -1;
                    //System.out.println(sto);
                    sMatrix.get(this.metabolitePositionMap.get(reactant)).put(this.reactionPositionMap.get(reaction.getId()), sto);
                }
            }

            for (String product : reaction.getProducts()) {
                if (this.metabolitesList.contains(product)) {
                    double sto = reaction.getStoichiometry(product);
                    sto = Math.abs(sto);
                   //  System.out.println(sto);
                    sMatrix.get(this.metabolitePositionMap.get(product)).put(this.reactionPositionMap.get(reaction.getId()), sto);
                }
            }
        }

        return sMatrix;
    }

    protected void setObjective() {
        //this.getSolver().setObjType(objType);
        // this.getSolver().setObjType(ObjType.Maximize);
        Map< Integer, Double> map = new HashMap<>();
        for (int i = 0; i < objectiveList.size(); i++) {
            if (objectiveList.get(i) != 0.0) {
                map.put(this.reactionPositionMap.get(this.reactionsList.get(i).getId()), 1.0);
            }
        }
        this.getSolver().setObj(map);
    }

    public void setModel(HashMap<String, ReactionFA> reactions, Model model) {        
        this.prepareReactions(reactions, model);
        this.getSolver().setObjType(this.direction);
    }
    
    public void setModel(HashMap<String, ReactionFA> reactions, Model model, ObjType objType) {
         this.getSolver().setObjType(objType);
        
        this.prepareReactions(reactions, model);
    }

    public void setSolverParameters() {
        this.setVars();
        this.setConstraints();
        this.setObjective();
    }

    public Map<String, Double> run() throws Exception {
        this.setSolverParameters();
        this.maxObj = this.getSolver().optimize();
        ArrayList<Double> fluxes = this.getSolver().getSoln();
        Map<String, Double> fluxesMap = new HashMap<>();
        for (ReactionFA reaction : this.reactionsList) {
            fluxesMap.put(reaction.getId(), fluxes.get(this.reactionPositionMap.get(reaction.getId())));
            reaction.setFlux(fluxes.get(this.reactionPositionMap.get(reaction.getId())));
        }
        //System.out.println("\n");
        return fluxesMap;
    }

    public abstract Solver getSolver();

    public double getMaxObj() {
        return this.maxObj;
    }

    private void prepareReactions(HashMap<String, ReactionFA> reactions, Model model) {
        this.reactionsList = new ArrayList<>();
        this.metabolitesList = new ArrayList<>();
        this.reactionPositionMap = new HashMap<>();
        this.objectiveList = new ArrayList<>();

        int i = 0;
        for (String reaction : reactions.keySet()) {
            // System.out.print(reaction + " - ");

            ReactionFA r = reactions.get(reaction);
            //if(r.getlb()==0 && r.getub()==0) continue;
            this.reactionsList.add(r);

            for (String reactant : r.getReactants()) {
                boolean sp = model.getSpecies(reactant).getBoundaryCondition();
                if (!sp) {
                    this.metabolitesList.add(reactant);
                }
            }
            for (String product : r.getProducts()) {
               // System.out.println(product);
                boolean sp = model.getSpecies(product).getBoundaryCondition();
                if (!sp) {
                    this.metabolitesList.add(product);
                }
            }
           //System.out.println(r.getId()+" - " +r.getObjective());
            if(r.getObjective()<0){
                this.direction = ObjType.Minimize;
            }
            this.objectiveList.add(r.getObjective());
            this.reactionPositionMap.put(r.getId(), i++);
        }

    }

}
