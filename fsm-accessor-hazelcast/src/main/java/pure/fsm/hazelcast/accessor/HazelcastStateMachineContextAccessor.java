package pure.fsm.hazelcast.accessor;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pure.fsm.core.Context;
import pure.fsm.core.accessor.StateMachineContextAccessor;
import pure.fsm.core.state.FinalState;
import pure.fsm.core.state.State;
import pure.fsm.core.state.StateFactory;
import pure.fsm.core.trait.Trait;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.ImmutableSet.copyOf;
import static java.util.stream.Collectors.toSet;
import static pure.fsm.core.Context.initialContext;
import static pure.fsm.core.context.MostRecentTrait.currentState;

public class HazelcastStateMachineContextAccessor implements StateMachineContextAccessor {

    private final Logger LOG = LoggerFactory.getLogger(HazelcastStateMachineContextAccessor.class);

    private final HazelcastInstance hazelcastInstance;

    public HazelcastStateMachineContextAccessor(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String create(State initialState, Class<? extends StateFactory> stateFactory, List<? extends Trait> initialTraits) {
        IAtomicLong idAtomicLong = getHazel().getAtomicLong("STATE_MACHINE_ID_GENERATOR");
        String id = String.valueOf(idAtomicLong.addAndGet(1));

        final Context context = initialContext(id, initialState, stateFactory, initialTraits);

        getHolderMap().put(id, context);

        return id;
    }

    @Override
    public Context get(String stateMachineId) {
        return getHolderMap().get(stateMachineId);
    }

    @Override
    public Optional<Lock> tryLock(String stateMachineId, long timeout, TimeUnit timeUnit) {
        java.util.concurrent.locks.Lock distributedLock = getHazel().getLock("STATE_MACHINE-" + stateMachineId);

        try {
            if (distributedLock.tryLock(timeout, timeUnit)) {
                return createLock(stateMachineId, distributedLock);
            }
        } catch (InterruptedException e) {
            LOG.warn("Could not get HZ distributed distributedLock for state machine [" + stateMachineId + "]", e);
        }
        return Optional.empty();
    }

    @Override
    public Set<String> getAllIds() {
        return copyOf(getHolderMap().keySet());
    }

    @Override
    public Set<String> getAllNonFinalIds() {
        return getHolderMap().entrySet().stream()
                .filter(e -> !FinalState.class.isAssignableFrom(currentState(e.getValue()).getClass()))
                .map(Map.Entry::getKey)
                .collect(toSet());
    }

    private Optional<Lock> createLock(String stateMachineId, java.util.concurrent.locks.Lock distributedLock) {
        Lock lock = new Lock() {
            @Override
            public Context getContext() {
                final Context context = getHolderMap().get(stateMachineId);
                return context;
            }

            @Override
            public void update(Context context) {
                getHolderMap().put(stateMachineId, context);
            }

            @Override
            public boolean unlock() {
                distributedLock.unlock();
                return true;
            }

            @Override
            public boolean unlockAndRemove() {
                unlock();
                getHolderMap().remove(stateMachineId);
                return true;
            }
        };

        return Optional.of(lock);
    }

    private IMap<String, Context> getHolderMap() {
        return getHazel().getMap("STATE_MACHINE_HOLDER");
    }

    private HazelcastInstance getHazel() {
        return hazelcastInstance;
    }
}
