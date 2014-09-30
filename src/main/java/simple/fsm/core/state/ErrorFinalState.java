package simple.fsm.core.state;

import simple.fsm.core.Context;
import simple.fsm.core.event.Event;

public class ErrorFinalState implements FinalState {

    private final Exception e;

    public ErrorFinalState(Exception e) {
        this.e = e;
    }

    @Override
    public State handle(Context context, Event event) {

        throw new IllegalStateException("In Error Final State, cannot process any more events");
    }

    @Override
    public void onExit(Context context, Event event) {

    }

    @Override
    public void onEntry(Context context, Event event, State prevState) {

    }
}
