package org.tdl.vireo.model.inheritence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.tdl.vireo.exception.ComponentNotPresentOnOrgException;
import org.tdl.vireo.exception.HeritableModelNonOverrideableException;
import org.tdl.vireo.exception.WorkflowStepNonOverrideableException;
import org.tdl.vireo.model.Organization;
import org.tdl.vireo.model.WorkflowStep;
import org.tdl.vireo.model.repo.OrganizationRepo;
import org.tdl.vireo.model.repo.WorkflowStepRepo;

@SuppressWarnings("rawtypes")
public class HeritableRepo<M extends HeritableComponent, R extends HeritableJpaRepo<M>> {

    @Autowired
    private R heritableRepo;

    @Autowired
    private WorkflowStepRepo workflowStepRepo;

    @Autowired
    private OrganizationRepo organizationRepo;

    public void removeFromWorkflowStep(Organization requestingOrganization, WorkflowStep pendingWorkflowStep, M heritableModelToRemove) throws WorkflowStepNonOverrideableException, HeritableModelNonOverrideableException, ComponentNotPresentOnOrgException {

        // if requesting organization originates the workflow step or the workflow step is overrideable,
        if (pendingWorkflowStep.getOriginatingOrganization().getId().equals(requestingOrganization.getId()) || pendingWorkflowStep.getOverrideable()) {
            // ... and if also that workflow step originates the heritableModel or the heritableModel is overrideable,
            if (heritableModelToRemove.getOriginatingWorkflowStep().getId().equals(heritableModelToRemove.getId()) || heritableModelToRemove.getOverrideable()) {
                // ...then the update is permissible.

                // if requesting organization is not the workflow step's orignating organization,
                if (!pendingWorkflowStep.getOriginatingOrganization().getId().equals(requestingOrganization.getId())) {
                    // create a new workflow step
                    pendingWorkflowStep = workflowStepRepo.update(pendingWorkflowStep, requestingOrganization);

                    // recursive call
                    pendingWorkflowStep.removeAggregateHeritableModel(heritableModelToRemove);

                    workflowStepRepo.save(pendingWorkflowStep);
                }
                // else, requesting organization originates the workflow step
                else {

                    List<WorkflowStep> workflowStepsContainingHeritableModel = getContainingDescendantWorkflowStep(requestingOrganization, heritableModelToRemove);

                    if (workflowStepsContainingHeritableModel.size() > 0) {

                        boolean foundNewOriginalOwner = false;

                        for (WorkflowStep workflowStepContainingHeritableModel : workflowStepsContainingHeritableModel) {
                            // add heritableModel as original to first workflow step
                            if (!foundNewOriginalOwner) {
                                workflowStepContainingHeritableModel.addOriginalHeritableModel(heritableModelToRemove);
                                foundNewOriginalOwner = true;
                            } else {
                                workflowStepContainingHeritableModel.addAggregateHeritableModel(heritableModelToRemove);
                            }
                            workflowStepRepo.save(workflowStepContainingHeritableModel);
                        }

                        pendingWorkflowStep.removeOriginalHeritableModel(heritableModelToRemove);

                        workflowStepRepo.save(pendingWorkflowStep);

                    } else {
                        heritableRepo.delete(heritableModelToRemove);
                    }
                }
            } // workflow step doesn't originate the heritableModel and it is non-overrideable
            else {
                throw new HeritableModelNonOverrideableException();
            }
        } // requesting org doesn't originate the heritableModel's workflow step, and the workflow step is non-overrideable
        else {
            throw new WorkflowStepNonOverrideableException();
        }
    }

    //Make change to component componentC at step stepS at org requestingOrganization
    @SuppressWarnings("unchecked")
    //@Transactional
    public M update(M componentCWithChanges, Organization requestingOrganization) throws ComponentNotPresentOnOrgException, WorkflowStepNonOverrideableException, HeritableModelNonOverrideableException {

        M resultingHeritableModel = null;

        Long phmId = componentCWithChanges.getId();

        M componentC = heritableRepo.findOne(phmId);

        boolean componentIsOverrideable = componentC.getOverrideable();

        boolean originalStepIsOverrideable = componentC.getOriginatingWorkflowStep().getOverrideable();
        
        boolean stepSIsOverrideable = false;

        // The ws that has the component on the requesting org
        WorkflowStep stepS= null;

        // Did the requesting organization originate the workflow step that the component is on?
        boolean requestingOrganizationOriginatedWorkflowStep = false;

        // Is the workflow step on which the component is found on the requesting organization the workflow step that originates the component?
        boolean workflowStepOriginatesComponent = false;

        for (WorkflowStep workflowStep : requestingOrganization.getAggregateWorkflowSteps()) {
            if (workflowStep.getAggregateHeritableModels(componentC.getClass()).contains(componentC)) {
                stepS = workflowStep;
                requestingOrganizationOriginatedWorkflowStep = stepS.getOriginatingOrganization().getId().equals(requestingOrganization.getId());
                stepSIsOverrideable = stepS.getOverrideable();
            }
        }
        
        if(stepSIsOverrideable != originalStepIsOverrideable)
            System.err.println("Inheritance problem - a non-overrideable step appears to have been overridden!");

        // A workflow step that has the heritableModel on it was found on the requesting organization
        if (stepS != null) {
            workflowStepOriginatesComponent = componentC.getOriginatingWorkflowStep().getId().equals(stepS.getId());
        } else {
            // The requesting org doesn't even have this heritableModel anywhere!
            throw new ComponentNotPresentOnOrgException();
        }

        if (!originalStepIsOverrideable && !requestingOrganizationOriginatedWorkflowStep) {
            throw new WorkflowStepNonOverrideableException();
        }

        if (!componentIsOverrideable && !(workflowStepOriginatesComponent && requestingOrganizationOriginatedWorkflowStep)) {
            throw new HeritableModelNonOverrideableException();
        }

        // If the requesting org originates the WS, then we don't need to make a new one
        if (requestingOrganizationOriginatedWorkflowStep) {

            // If the WS originates component C, we don't need a new one
            if (workflowStepOriginatesComponent) {

                // update heritableModel directly
                resultingHeritableModel = heritableRepo.save(componentCWithChanges);

                // if change to non-overrideable, replace descendants of this heritableModel in subordinate orgs and put it back on ones that deleted it
                if (componentIsOverrideable && !resultingHeritableModel.getOverrideable()) {
                    reInheritDescendantsOfHeritableModelWithAnotherHeritableModelUnderWS(componentC, resultingHeritableModel, stepS, requestingOrganization);
                }
            }
            // If the WS didn't originate the M, we need a new M to replace it
            else {
                System.out.println("\n\nRequesting org " + requestingOrganization.getName() + " originated workflow step where " + componentC.getId() + " appears on it, but that workflow step " + stepS.getName() + "(" + stepS.getId() + ") didn't originate the component.  Making a new component." );

                M cloneHeritableModel = (M) componentCWithChanges.clone();

                cloneHeritableModel.setOriginating(componentC);
                cloneHeritableModel.setOriginatingWorkflowStep(stepS);

                M newHeritableModel = heritableRepo.save(cloneHeritableModel);

                requestingOrganization = organizationRepo.findOne(requestingOrganization.getId());

                //TODO:  test
                //in component aggregations at descendants of the WS, make components derived from pendingHeritableModel now originate from the clone
                for (WorkflowStep workflowStep : workflowStepRepo.getDescendantsOfStepUnderOrganization(stepS, requestingOrganization)) {
                    System.out.println("\tWS " + workflowStep.getName() + "(" + workflowStep.getId() + ") descends from stepS " + stepS.getName() + "(" + stepS.getId() + " under requesting org " + requestingOrganization.getName() + "(" + requestingOrganization.getId() + ")");
                    List<M> aggregatedComponents = workflowStep.getAggregateHeritableModels(componentC.getClass());
                    
                    List<M> modelsToChangeInheritance = new ArrayList<M>();
                            
                    for(M model : aggregatedComponents) {
                        System.out.println("\t\tComponent " + model.getId() + " is aggregated there.");
                        if(model.getOriginating() != null && model.getOriginating().equals(componentC)) {
                            System.out.println("It originated at the component being overridden, so setting it's new originator to the override.");
                            modelsToChangeInheritance.add(model);                     
                        }                        
                    }
                    for(M model : modelsToChangeInheritance) {
                        model.setOriginating(newHeritableModel);
                        heritableRepo.save(model);
                    }
                }
                
                // replace descendants of the persisted (original) M with our new M at subordinate organizations
                // replace the heritableModel on all descendant orgs aggregate workflows
                for (WorkflowStep workflowStep : getContainingDescendantWorkflowStep(requestingOrganization, componentC)) {
                    workflowStep.replaceAggregateHeritableModel(componentC, newHeritableModel);
                    workflowStepRepo.save(workflowStep);
                }

                // if change to non-overrideable, replace descendants of originating heritableModel in subordinate orgs
                if (componentIsOverrideable && !newHeritableModel.getOverrideable()) {
                    reInheritDescendantsOfHeritableModelWithAnotherHeritableModelUnderWS(componentC, newHeritableModel, stepS, requestingOrganization);
                }

                resultingHeritableModel = newHeritableModel;
            }
        }
        // If the requesting org didn't originate the WS, we need a new WS to replace it and to originate a new M
        // workflowStepWithHeritableModelOnRequestingOrganization does not originate on the requesting org
        else {
            System.out.println("\n\nRequesting org " + requestingOrganization.getName() + " didn't originate component " + componentC.getId() + "'s originating WS of " + stepS.getName() + "(" + stepS.getId() + ")" );

            WorkflowStep stepT = workflowStepRepo.update(stepS, requestingOrganization);
            System.out.println("Created new step " + stepT.getId() + " to override step " + stepS.getId() + " at org " + requestingOrganization.getName() + "(" + requestingOrganization.getId() + ")");
                    
            Long stepTId = stepT.getId();

            M cloneHeritableModel = (M) componentCWithChanges.clone();

            cloneHeritableModel.setOriginating(componentC);
            cloneHeritableModel.setOriginatingWorkflowStep(stepT);

            
            M newHeritableModel = heritableRepo.save(cloneHeritableModel);
            System.out.println("Created new component " + newHeritableModel.getId());

            stepT = workflowStepRepo.findOne(stepT.getId());

            stepT.getOriginalHeritableModels(newHeritableModel.getClass()).add(newHeritableModel);

            stepT.replaceAggregateHeritableModel(componentC, newHeritableModel);

            stepT = workflowStepRepo.save(stepT);

            stepS = workflowStepRepo.findOne(stepS.getId());

            requestingOrganization.replaceAggregateWorkflowStep(stepS, stepT);

            requestingOrganization = organizationRepo.save(requestingOrganization);
            stepT = workflowStepRepo.findOne(stepTId);
            
            //TODO:  test
            // in component aggregations at descendants of new WS under descendants of requesting org, make components derived from the pending now inherit from the clone
            Set<M> componentsToSave = new HashSet<M>(); 
            Set<WorkflowStep> stepsToSave = new HashSet<WorkflowStep>();
            for(Organization descendantOrg : organizationRepo.getDescendantOrganizations(requestingOrganization)) {
                System.out.println("\tProcessing descendants of Org " + requestingOrganization.getName());
                for(WorkflowStep descendantStep : descendantOrg.getOriginalWorkflowSteps()) { //TODO:  was getAggregate
                    System.out.println("\t\tProcessing descendant org's  WS " + descendantStep.getName());
                    List<M> components = descendantStep.getOriginalHeritableModels(componentCWithChanges.getClass()); //TODO:  was getAggregate
                    for( M component : components) {
                        
                        M originating = (M) component.getOriginating();
                        System.out.println("\t\t\tProcessing components of type " + component.getClass().getName() + " id " + component.getId() + " originated by " + ( originating != null ? originating.getId() : "none")  );
                        if(originating != null && originating.equals(componentC) && component.getId() != newHeritableModel.getId() ) {
                            System.out.println("\t\t\t\tComponent " + component.getId() + " originated at the overridden heritable model, make it's new orignator the new heritable model with id " + newHeritableModel.getId());
                            component.setOriginating(newHeritableModel);
                            stepsToSave.add(descendantStep);
                            componentsToSave.add(component);
                        }
                    }
                }
            }
            for(M component : componentsToSave) {
                heritableRepo.save(component);
            }
            for(WorkflowStep descendantStep : stepsToSave) {
                //TODO:
                workflowStepRepo.save(descendantStep);
            }

            // replace the heritableModel on all descendant orgs aggregate workflows
            
            for (WorkflowStep workflowStep : getContainingDescendantWorkflowStep(requestingOrganization, componentC)) {
                System.out.println("Replacing component " + componentC.getId() + " with new component " + newHeritableModel.getId() + " on WS " + workflowStep.getId());
                workflowStep.replaceAggregateHeritableModel(componentC, newHeritableModel);
                //TODO:
                workflowStepRepo.save(workflowStep);
            }

            // if change to non-overrideable, replace descendants of originating heritableModel in subordinate orgs
            if (componentIsOverrideable && !newHeritableModel.getOverrideable()) {
                reInheritDescendantsOfHeritableModelWithAnotherHeritableModelUnderWS(componentC, newHeritableModel, stepS, requestingOrganization);
            }

            resultingHeritableModel = newHeritableModel;
        }

        
        //TODO:  cleanup.  should not have to do this.
        organizationRepo.cleanHierarchy();
        
        return resultingHeritableModel;

    }

    @SuppressWarnings("unchecked")
    public void delete(M heritableModel) {

        // allows for delete by iterating through findAll, while still deleting descendents
        if (heritableRepo.findOne(heritableModel.getId()) != null) {

            WorkflowStep originatingWorkflowStep = heritableModel.getOriginatingWorkflowStep();

            originatingWorkflowStep.removeOriginalHeritableModel(heritableModel);

            if (heritableModel.getOriginating() != null) {
                heritableModel.setOriginating(null);
            }

            workflowStepRepo.findByAggregateHeritableModel(heritableModel).forEach(workflowStep -> {
                workflowStep.removeAggregateHeritableModel(heritableModel);
                workflowStepRepo.save(workflowStep);
            });

            heritableRepo.findByOriginating(heritableModel).forEach(fp -> {
                fp.setOriginating(null);
            });

            deleteDescendantsOfHeritableModel(heritableModel);

            heritableRepo.delete(heritableModel.getId());

        }
    }

    private void deleteDescendantsOfHeritableModel(M heritableModel) {
        heritableRepo.findByOriginating(heritableModel).forEach(desendantHeritableModel -> {
            System.out.println("Deleting component " + desendantHeritableModel.getId() + " off step  " + desendantHeritableModel.getOriginatingWorkflowStep().getName());
            delete(desendantHeritableModel);
        });
    }

    /**
     * Gets a list of WorkflowSteps on the org and its descendants that contain a given heritableModel
     *
     * @param organization
     * @param heritableModel
     * @return
     */
    private List<WorkflowStep> getContainingDescendantWorkflowStep(Organization organization, M heritableModel) {
        List<WorkflowStep> descendantWorkflowStepsContainingHeritableModel = new ArrayList<WorkflowStep>();
        organization.getAggregateWorkflowSteps().forEach(ws -> {
            if (ws.getAggregateHeritableModels(heritableModel.getClass()).contains(heritableModel)) {
                descendantWorkflowStepsContainingHeritableModel.add(ws);
            }
        });
        organization.getChildrenOrganizations().forEach(descendantOrganization -> {
            descendantWorkflowStepsContainingHeritableModel.addAll(getContainingDescendantWorkflowStep(descendantOrganization, heritableModel));
        });
        return descendantWorkflowStepsContainingHeritableModel;
    }

    // TODO: same logic here as in WorkflowStepRepoImpl.getDescendantsOfStep
    private List<M> getDescendantsOfHeritableModel(M heritableModel) {
        List<M> descendantHeritableModels = new ArrayList<M>();
        List<M> currentDescendants = heritableRepo.findByOriginating(heritableModel);
        descendantHeritableModels.addAll(currentDescendants);
        currentDescendants.forEach(descendantHeritableModel -> {
            descendantHeritableModels.addAll(getDescendantsOfHeritableModel(descendantHeritableModel));
        });
        return descendantHeritableModels;
    }

    /**
     * Have all the heritableModels (in workflow steps descended from a given step) that are derived from a particular ancestor heritableModel be replaced with a replacement heritableModel (which could also be just that ancestor heritableModel)
     *
     */

    @SuppressWarnings("unchecked")
    private void reInheritDescendantsOfHeritableModelWithAnotherHeritableModelUnderWS(M ancestorHeritableModel, M replacementHeritableModel, WorkflowStep workflowStepWithHeritableModelOnRequestingOrganization, Organization requestingOrganization) {

        // First off, heritableModel the HeritableModels that descend from the ancestor heritableModel
        List<M> descendantHeritableModels = getDescendantsOfHeritableModel(ancestorHeritableModel);

        // For every workflow step derived off the step in question...

        // for(WorkflowStep ws : workflowStepRepo.getDescendantsOfStep(workflowStepWithHeritableModelOnRequestingOrganization)) {
        List<M> heritableModelsToDelete = new ArrayList<M>();

        for (WorkflowStep ws : workflowStepRepo.getDescendantsOfStepUnderOrganization(workflowStepWithHeritableModelOnRequestingOrganization, requestingOrganization)) {

            boolean aggregatesHeritableModelOrDescendant = ws.getAggregateHeritableModels(replacementHeritableModel.getClass()).contains(replacementHeritableModel);

            // For every heritableModel on that step (the aggregates will include the originals)
            List<M> copyOfAggregatedHeritableModels = new ArrayList<M>();

            copyOfAggregatedHeritableModels.addAll(ws.getAggregateHeritableModels(replacementHeritableModel.getClass()));

            for (M n : copyOfAggregatedHeritableModels) {

                // If that heritableModel is a descendant of the heritableModel in question, replace it with the heritableModel in question and get rid of it
                if (descendantHeritableModels.contains(n) && !replacementHeritableModel.equals(n)) {

                    if (ws.replaceAggregateHeritableModel(n, replacementHeritableModel)) {
                        ws.removeOriginalHeritableModel(n);
                        heritableModelsToDelete.add(n);
                        aggregatesHeritableModelOrDescendant = true;
                    }
                }
            }

            // If the heritableModel was not found on the aggregates at all, then add it back in
            if (!aggregatesHeritableModelOrDescendant) {
                ws.addAggregateHeritableModel(replacementHeritableModel);
            }

            workflowStepRepo.save(ws);
        }

        for (M n : heritableModelsToDelete) {
            delete(n);
        }
    }

}
