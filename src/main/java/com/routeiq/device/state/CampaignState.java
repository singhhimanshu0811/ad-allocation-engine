package com.routeiq.device.state;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum CampaignState {
    DRAFT,
    SCHEDULED,
    RUNNING,
    PAUSED,
    STOPPED;

    private static final Map<CampaignState, Set<CampaignState>> TRANSITIONS = Map.of(
        DRAFT, EnumSet.of(SCHEDULED),
        SCHEDULED, EnumSet.of(STOPPED, RUNNING),
        RUNNING, EnumSet.of(PAUSED, STOPPED),
        STOPPED, EnumSet.of(DRAFT),
        PAUSED, EnumSet.of(STOPPED, RUNNING)
    );

    public boolean canTransitionTo(CampaignState next) {
        return next != null && TRANSITIONS.get(this).contains(next);
    }

    public Set<CampaignState> allowedTransitions() {
        return TRANSITIONS.get(this);
    }
}
