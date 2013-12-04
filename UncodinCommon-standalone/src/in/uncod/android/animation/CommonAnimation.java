package in.uncod.android.animation;

import android.view.View;

import com.nineoldandroids.animation.AnimatorListenerAdapter;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

public class CommonAnimation {

    public static void crossfade(View show, final View hide, int animationDuration) {
        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        animate(show).alpha(0f).setDuration(0);
        show.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        animate(show)
                .alpha(1f)
                .setDuration(animationDuration)
                .setListener(null);

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        animate(hide)
                .alpha(0f)
                .setDuration(animationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(com.nineoldandroids.animation.Animator animation) {
                        hide.setVisibility(View.GONE);
                    }
                });
    }

}
