package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.StopMove;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.FreeAttack;
import games.descent2e.actions.attack.RangedAttack;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;

import java.util.ArrayList;
import java.util.List;

import static games.descent2e.DescentHelper.moveActions;
import static games.descent2e.actions.Triggers.ANYTIME;
import static games.descent2e.actions.herofeats.DoubleMoveAttack.DoubleMoveAttackPhase.*;
import static games.descent2e.actions.herofeats.DoubleMoveAttack.Interrupters.*;

public class DoubleMoveAttack extends DescentAction implements IExtendedSequence {

    // Jain Fairwood Heroic Feat
    String heroName = "Jain Fairwood";

    enum Interrupters {
        HERO, OTHERS, ALL
    }

    public enum DoubleMoveAttackPhase {
        NOT_STARTED,
        PRE_HERO_ACTION(ANYTIME, HERO),
        POST_HERO_ACTION,
        ALL_DONE;

        public final Triggers interrupt;
        public final DoubleMoveAttack.Interrupters interrupters;

        DoubleMoveAttackPhase(Triggers interruptType, DoubleMoveAttack.Interrupters who) {
            interrupt = interruptType;
            interrupters = who;
        }

        DoubleMoveAttackPhase() {
            interrupt = null;
            interrupters = null;
        }
    }

    DoubleMoveAttackPhase phase = NOT_STARTED;
    int heroPlayer;
    int interruptPlayer;
    Hero hero;
    int oldMovePoints;
    boolean oldFreeAttack;

    public DoubleMoveAttack(Hero hero) {
        super(Triggers.HEROIC_FEAT);
        this.hero = hero;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Heroic Feat: " + heroName + " moves twice her speed and can attack anytime during it.";
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        if (phase.interrupt == null)
            throw new AssertionError("Should not be reachable");
        DescentGameState dgs = (DescentGameState) state;
        List<AbstractAction> retVal = new ArrayList<>();
        List<AbstractAction> moveActions = new ArrayList<>();
        List<RangedAttack> attacks = new ArrayList<>();;

        //System.out.println("Jain has attacked yet: " + hero.hasUsedHeroAbility());
        if (!hero.hasUsedExtraAction())
        {
            List<Integer> targets = DescentHelper.getRangedTargets(dgs, hero);
            if (!targets.isEmpty())
                for (Integer target : targets)
                    attacks.add(new FreeAttack(hero.getComponentID(), target, false));
        }

        switch (phase) {
            case PRE_HERO_ACTION:
                moveActions = moveActions(dgs, hero);
                if (!moveActions.isEmpty())
                {
                    retVal.add(new StopMove(hero));
                    retVal.addAll(moveActions);
                }
                if (!attacks.isEmpty())
                    retVal.addAll(attacks);
                break;
            default:
                break;
        }

        return retVal;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return interruptPlayer;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        // after the interrupt action has been taken, we can continue to see who interrupts next
        movePhaseForward((DescentGameState) state);
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return (phase == ALL_DONE);
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        dgs.setActionInProgress(this);
        heroPlayer = dgs.getCurrentPlayer();
        interruptPlayer = heroPlayer;
        oldMovePoints = hero.getAttribute(Figure.Attribute.MovePoints).getValue();
        oldFreeAttack = hero.hasUsedExtraAction();
        phase = PRE_HERO_ACTION;

        // Jain can move up to double her speed, and attack at any point during the move (before, during and after)
        hero.setAttribute(Figure.Attribute.MovePoints, hero.getAttributeMax(Figure.Attribute.MovePoints) * 2);
        hero.setUsedExtraAction(false);

        movePhaseForward(dgs);

        return true;
    }

    private void movePhaseForward(DescentGameState state) {
        // The goal here is to work out which player may have an interrupt for the phase we are in
        // If none do, then we can move forward to the next phase directly.
        // If one (or more) does, then we stop, and go back to the main game loop for this
        // decision to be made
        boolean foundInterrupt = false;
        do {
            if (playerHasInterruptOption(state)) {
                foundInterrupt = true;
                // System.out.println("Heroic Feat Interrupt: " + phase + ", Interrupter:" + phase.interrupters + ", Interrupt:" + phase.interrupt + ", Player: " + interruptPlayer);
                // we need to get a decision from this player
            } else {
                interruptPlayer = (interruptPlayer + 1) % state.getNPlayers();
                if (phase.interrupt == null || interruptPlayer == heroPlayer) {
                    // we have completed the loop, and start again with the attacking player
                    executePhase(state);
                    interruptPlayer = heroPlayer;
                }
            }
        } while (!foundInterrupt && phase != ALL_DONE);
    }

    private boolean playerHasInterruptOption(DescentGameState state) {
        if (phase.interrupt == null || phase.interrupters == null) return false;
        // first we see if the interruptPlayer is one who may interrupt
        switch (phase.interrupters) {
            case HERO:
                if (interruptPlayer != heroPlayer)
                    return false;
                break;
            case OTHERS:
                if (interruptPlayer == heroPlayer)
                    return false;
                break;
            case ALL:
                // always fine
        }
        // second we see if they can interrupt (i.e. have a relevant card/ability)
        return !_computeAvailableActions(state).isEmpty();
    }

    private void executePhase(DescentGameState state) {
        // System.out.println("Executing phase " + phase);
        switch (phase) {
            case NOT_STARTED:
            case ALL_DONE:
                // TODO Fix this temporary solution: it should not keep looping back to ALL_DONE, put the error back in
                break;
            //throw new AssertionError("Should never be executed");
            case PRE_HERO_ACTION:
                phase = POST_HERO_ACTION;
                break;
            case POST_HERO_ACTION:
                hero.setAttribute(Figure.Attribute.MovePoints, oldMovePoints);
                hero.setUsedExtraAction(oldFreeAttack);
                hero.setFeatAvailable(false);
                hero.getNActionsExecuted().increment();
                phase = ALL_DONE;
                break;
        }
        // and reset interrupts
    }

    @Override
    public DoubleMoveAttack copy() {
        DoubleMoveAttack retValue = new DoubleMoveAttack(hero);
        retValue.phase = phase;
        retValue.heroPlayer = heroPlayer;
        retValue.interruptPlayer = interruptPlayer;
        retValue.oldMovePoints = oldMovePoints;
        retValue.oldFreeAttack = oldFreeAttack;
        return retValue;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return hero.getName().contains(heroName) && hero.isFeatAvailable();
    }
}
