package com.vtt.antnd.modules.simulation.FBA.LP.minimize;


import com.vtt.antnd.data.antSimData.ReactionFA;
import com.vtt.antnd.modules.simulation.FBA.LP.ConType;
import com.vtt.antnd.modules.simulation.FBA.LP.ObjType;
import com.vtt.antnd.modules.simulation.FBA.LP.Solver;
import com.vtt.antnd.modules.simulation.FBA.LP.VarType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.sbml.jsbml.Model;

public abstract class Analysism {

    protected double maxObj = Double.NaN;
    List< Double> objectiveList;
    private Map<String, Integer> reactionPositionMap;
    private Map<String, Integer> metabolitePositionMap;
    List<ReactionFA> reactionsList;
    List<String> metabolitesList;

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
                    sMatrix.get(this.metabolitePositionMap.get(reactant)).put(this.reactionPositionMap.get(reaction.getId()), sto);
                }
            }

            for (String product : reaction.getProducts()) {
                if (this.metabolitesList.contains(product)) {
                    double sto = reaction.getStoichiometry(product);
                    sto = Math.abs(sto);
                    sMatrix.get(this.metabolitePositionMap.get(product)).put(this.reactionPositionMap.get(reaction.getId()), sto);
                }
            }
        }

        return sMatrix;
    }

    protected void setObjective() {    
        this.getSolver().setObjType(ObjType.Minimize);
        Map< Integer, Double> map = new HashMap<>();
        for (int i = 0; i < objectiveList.size(); i++) {
            map.put(this.reactionPositionMap.get(this.reactionsList.get(i).getId()), objectiveList.get(i));
        }

        this.getSolver().setObj(map);
    }

    public void setModel(HashMap<String, ReactionFA> reactions, Model model, Double objective) {
        this.getSolver().setObjType(ObjType.Minimize);
        this.prepareReactions(reactions, model, objective);
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
        for (ReactionFA reaction : this.reactionsList) {
            if (reaction.getId().contains("R") && Math.abs(reaction.getFlux()) > 0.00000001) {
                setFlux(reaction.getId(), reaction.getFlux(), fluxesMap);
            }
        }

        return fluxesMap;
    }

    public abstract Solver getSolver();

    public double getMaxObj() {
        return this.maxObj;
    }

    private void prepareReactions(HashMap<String, ReactionFA> reactions, Model model, double objective) {
        this.reactionsList = new ArrayList<>();
        this.metabolitesList = new ArrayList<>();
        this.reactionPositionMap = new HashMap<>();
        this.objectiveList = new ArrayList<>();

        int i = 0;
        for (String reaction : reactions.keySet()) {
            ReactionFA r = reactions.get(reaction);
            if (r.getlb() < 0 && r.getub() > 0) {
                try {
                    ReactionFA newR = r.clone();
                    newR.reverseReaction();
                    newR.setBounds(0, Math.abs(r.getlb()));
                    r.setBounds(0.0, r.getub());
                    
                    this.reactionsList.add(newR);
                    this.reactionPositionMap.put(newR.getId(), i++);
                    this.objectiveList.add(1.0);
                } catch (CloneNotSupportedException ex) {
                    Logger.getLogger(Analysism.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
         
            this.reactionsList.add(r);

            for (String reactant : r.getReactants()) {                
                if (!model.getSpecies(reactant).getBoundaryCondition()) {
                    this.metabolitesList.add(reactant);
                }
            }
            for (String product : r.getProducts()) {               
                if (!model.getSpecies(product).getBoundaryCondition()) {
                    this.metabolitesList.add(product);
                }
            }

            this.reactionPositionMap.put(r.getId(), i++);
            if (r.getObjective() == 1) {
                r.setBounds(objective, objective);
            }
           
            this.objectiveList.add(1.0);

        }

    }

    private void setFlux(String id, double flux, Map<String, Double> fluxesMap) {
        String realId = id.substring(0, id.length() - 1);
        for (ReactionFA r : this.reactionsList) {
            if (r.getId().equals(realId)) {
                r.setFlux(flux * -1);
                fluxesMap.put(r.getId(), flux * -1);
            }
        }
    }

}
