package in.appoholics.the3s;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

final class EdgeToEdgeHelper {

    private EdgeToEdgeHelper() {
    }

    static void apply(
            @NonNull AppCompatActivity activity,
            int rootViewId,
            @Nullable Integer topInsetTargetId,
            @Nullable Integer bottomInsetTargetId
    ) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

        View root = activity.findViewById(rootViewId);
        if (root == null) {
            return;
        }

        View topInsetTarget = topInsetTargetId != null ? activity.findViewById(topInsetTargetId) : null;
        View bottomInsetTarget = bottomInsetTargetId != null ? activity.findViewById(bottomInsetTargetId) : null;

        final int topPaddingStart = topInsetTarget != null ? topInsetTarget.getPaddingTop() : 0;
        final int bottomPaddingStart = bottomInsetTarget != null ? bottomInsetTarget.getPaddingBottom() : 0;

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            if (topInsetTarget != null) {
                topInsetTarget.setPadding(
                        topInsetTarget.getPaddingLeft(),
                        topPaddingStart + systemBars.top,
                        topInsetTarget.getPaddingRight(),
                        topInsetTarget.getPaddingBottom()
                );
            }

            if (bottomInsetTarget != null) {
                bottomInsetTarget.setPadding(
                        bottomInsetTarget.getPaddingLeft(),
                        bottomInsetTarget.getPaddingTop(),
                        bottomInsetTarget.getPaddingRight(),
                        bottomPaddingStart + systemBars.bottom
                );
            }

            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }
}
