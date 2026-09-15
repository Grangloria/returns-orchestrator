package com.grangloria.returns.entity;

import java.util.Set;

public enum ReturnState {

    INITIATED {
        @Override
        public Set<ReturnState> nextValidStates() {
            return Set.of(LABEL_READY, FAILED, CANCELLED);
        }
    },

    LABEL_READY {
        @Override
        public Set<ReturnState> nextValidStates() {
            return Set.of(IN_TRANSIT, CANCELLED);
        }
    },

    IN_TRANSIT {
        @Override
        public Set<ReturnState> nextValidStates() {
            return Set.of(RECEIVED);
        }
    },

    RECEIVED {
        @Override
        public Set<ReturnState> nextValidStates() {
            return Set.of(REFUNDED, COMPLETED);
        }
    },

    REFUNDED {
        @Override
        public Set<ReturnState> nextValidStates() {
            return Set.of(COMPLETED);
        }
    },

    COMPLETED {
        @Override
        public Set<ReturnState> nextValidStates() {
            return Set.of(); // Terminal state
        }
    },

    FAILED {
        @Override
        public Set<ReturnState> nextValidStates() {
            return Set.of(INITIATED); // Can retry initiation
        }
    },

    CANCELLED {
        @Override
        public Set<ReturnState> nextValidStates() {
            return Set.of(); // Terminal state
        }
    };

    /**
     * Defines allowed downstream transitions
     */
    public abstract Set<ReturnState> nextValidStates();

    /**
     * Guard method to check if a state transition is legal
     */
    public boolean canTransitionTo(ReturnState targetState) {
        return nextValidStates().contains(targetState);
    }
}