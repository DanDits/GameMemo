package dan.dit.gameMemo.appCore.binokel;

import dan.dit.gameMemo.appCore.GameDetailActivity;

/**
 * Created by daniel on 26.01.16.
 */
public class BinokelGameDetailActivity extends GameDetailActivity {
    @Override
    public void onBackPressed() {
        boolean businessDone = false;
        if (mDetails != null) {
            BinokelGameDetailFragment frag = (BinokelGameDetailFragment) mDetails;
            if (frag.hasSelectedRound()) {
                frag.deselectRound();
                businessDone = true;
            }
        }
        if (!businessDone) {
            super.onBackPressed();
        }
    }
}
