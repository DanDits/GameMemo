package dan.dit.gameMemo.appCore.doppelkopf;

import dan.dit.gameMemo.appCore.GameDetailActivity;

public class DoppelkopfGameDetailActivity extends GameDetailActivity {

    @Override
    public void onBackPressed() {
        boolean businessDone = false;
        if (mDetails != null) {
            DoppelkopfGameDetailFragment frag = (DoppelkopfGameDetailFragment) mDetails;
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
