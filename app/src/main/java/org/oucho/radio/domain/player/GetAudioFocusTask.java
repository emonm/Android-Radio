package org.oucho.radio.domain.player;

import view.activities.HomeActivity;
import view.fragment.RadioFragment;

public class GetAudioFocusTask implements Runnable {
    final HomeActivity context;


    public GetAudioFocusTask(HomeActivity context) {
        this.context = context;
    }

    public void run() {

        RadioFragment.stop(context);

    }
}
