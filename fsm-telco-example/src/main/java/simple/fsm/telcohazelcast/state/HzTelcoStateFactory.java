package simple.fsm.telcohazelcast.state;

import simple.fsm.core.state.BaseStateFactory;
import simple.fsm.core.state.State;
import simple.fsm.hazelcast.resource.DistributedResourceFactory;
import simple.fsm.telco.state.RechargeRequestedState;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class HzTelcoStateFactory extends BaseStateFactory {

    private final Map<Class<? extends State>, State> stateByStateClass = newHashMap();

    public HzTelcoStateFactory(DistributedResourceFactory distributedResourceFactory) {
        super();
        stateByStateClass.put(HzInitialState.class, new HzInitialState(distributedResourceFactory));
        stateByStateClass.put(RechargeRequestedState.class, new RechargeRequestedState());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends State> T internalGetStateByClass(Class<T> stateClass) {
        return (T) stateByStateClass.get(stateClass);
    }
}