package io.sweers.tictactoe;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.getSize;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static io.sweers.tictactoe.TicTacToeGame.PLAYER_ONE;
import static io.sweers.tictactoe.TicTacToeGame.PLAYER_TWO;

/**
 * A view representing a tic tac toe grid
 */
public final class TicTacToeView extends FrameLayout {

    public interface OnTileClickListener {
        void onTileClick(int position);
    }

    private static final ButterKnife.Action<ImageView> DISABLE = new ButterKnife.Action<ImageView>() {
        @Override
        public void apply(ImageView tile, int index) {
            tile.setEnabled(false);
            if (tile.getDrawable() != null) {
                safeTint(tile, R.color.disabled);
            }
        }
    };
    private static final ButterKnife.Action<ImageView> ENABLE_BLANKS = new ButterKnife.Action<ImageView>() {
        @Override
        public void apply(ImageView tile, int index) {
            boolean hasDrawable = tile.getDrawable() != null;
            tile.setEnabled(!hasDrawable);
            if (hasDrawable) {
                safeTint(tile, R.color.primary);
            }
        }
    };
    private static final ButterKnife.Action<ImageView> ENABLE_ALL = new ButterKnife.Action<ImageView>() {
        @Override
        public void apply(ImageView tile, int index) {
            tile.setEnabled(true);
            safeTint(tile, R.color.primary);
        }
    };
    private static final ButterKnife.Action<ImageView> RESET_IMAGE = new ButterKnife.Action<ImageView>() {
        @Override
        public void apply(ImageView tile, int index) {
            tile.setImageDrawable(null);
        }
    };

    private final Paint paint = new Paint();

    @Bind({
            R.id.zero,
            R.id.one,
            R.id.two,
            R.id.three,
            R.id.four,
            R.id.five,
            R.id.six,
            R.id.seven,
            R.id.eight,
    })
    protected List<ImageView> tiles;

    private char nextPlayer;
    private OnTileClickListener tileClickListener;

    public TicTacToeView(Context context) {
        super(context);
    }

    public TicTacToeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TicTacToeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TicTacToeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Utility function for tinting. I opted against using the support library's tint backport
     * because it exhibits weird race conditions on pre-lollipop when you need it to happen
     * synchronously.
     */
    private static void safeTint(ImageView imageView, @ColorRes int color) {
        int resolvedColor = imageView.getResources().getColor(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            imageView.setImageTintList(ColorStateList.valueOf(resolvedColor));
        } else {
            Drawable drawable = imageView.getDrawable();
            if (drawable != null) {
                drawable.setColorFilter(resolvedColor, PorterDuff.Mode.SRC_IN);
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setWillNotDraw(false);
        ButterKnife.bind(this);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.stroke_width));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int originalWidth = getSize(widthMeasureSpec);
        int originalHeight = getSize(heightMeasureSpec);

        int width = originalWidth - getPaddingLeft() - getPaddingRight();
        int height = originalHeight - getPaddingTop() - getPaddingBottom();

        int limitingFactor = Math.min(width, height);

        int tileSize = limitingFactor / 3;

        for (ImageView tile : tiles) {
            tile.measure(makeMeasureSpec(tileSize, EXACTLY), makeMeasureSpec(tileSize, EXACTLY));
        }

        int size = limitingFactor == width ? originalWidth : originalHeight;
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int currentLeft = getPaddingLeft();
        int currentTop = getPaddingTop();

        for (int i = 0; i < tiles.size(); i++) {
            ImageView tile = tiles.get(i);
            int measuredWidth = tile.getMeasuredWidth();
            int measuredHeight = tile.getMeasuredHeight();
            tile.layout(currentLeft, currentTop, currentLeft + measuredWidth, currentTop + measuredHeight);
            currentLeft += measuredWidth;

            if ((i + 1) % 3 == 0) {
                // Move to the next row
                currentLeft = getPaddingLeft();
                currentTop += measuredHeight;
            }
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int size = Math.min(getWidth(), getHeight());
        int increment = size / 3;

        // Draw horizontal lines
        int y = increment;
        canvas.drawLine(0, y, size , y, paint);
        y += increment;
        canvas.drawLine(0, y, size, y, paint);

        // Draw vertical lines
        int x = increment;
        canvas.drawLine(x, 0, x, size, paint);
        x += increment;
        canvas.drawLine(x, 0, x, size, paint);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        // We want to disable tile clicks when disabled
        if (enabled) {
            ButterKnife.apply(tiles, ENABLE_BLANKS);
        } else {
            ButterKnife.apply(tiles, DISABLE);
        }
    }

    public void setOnTileClickedListener(OnTileClickListener listener) {
        this.tileClickListener = listener;
    }

    public void setNextPlayer(char nextPlayer) {
        this.nextPlayer = nextPlayer;
    }

    @OnClick({
            R.id.zero,
            R.id.one,
            R.id.two,
            R.id.three,
            R.id.four,
            R.id.five,
            R.id.six,
            R.id.seven,
            R.id.eight,
    })
    protected void onTileClicked(final ImageView tile) {
        if (tileClickListener != null) {
            setTile(tile, nextPlayer);

            // Give the tile time to settle before moving forward, to make sure tints can properly apply
            post(new Runnable() {
                @Override
                public void run() {
                    tileClickListener.onTileClick(tiles.indexOf(tile));
                }
            });
        }
    }

    public void setTile(int index, char player) {
        setTile(tiles.get(index), player);
    }

    private void setTile(ImageView tile, char player) {
        @DrawableRes int resId;
        if (player == PLAYER_ONE) {
            resId = R.drawable.x;
        } else if (player == PLAYER_TWO) {
            resId = R.drawable.circle;
        } else {
            resId = -1;
        }

        if (resId != -1) {
            tile.setEnabled(false);
            tile.setImageResource(resId);
        } else {
            tile.setEnabled(true);
            tile.setImageDrawable(null);
        }
    }

    public void endGame(@Nullable final int[] winningIndices) {
        ButterKnife.apply(tiles, DISABLE);
        if (winningIndices != null) {
            boolean[] allIndices = {false, false, false, false, false, false, false, false, false};
            for (int i : winningIndices) {
                allIndices[i] = true;
            }
            for (int i = 0; i < allIndices.length; i++) {
                ImageView tile = tiles.get(i);
                safeTint(tile, allIndices[i] ? R.color.accent : R.color.disabled);
                tile.setEnabled(false);
            }
        }
    }

    public void reset() {
        // https://github.com/JakeWharton/butterknife/issues/332
        ButterKnife.apply(tiles, ENABLE_ALL);
        ButterKnife.apply(tiles, RESET_IMAGE);
    }

    public void restoreBoard(char[] gridState) {
        for (int i = 0; i < tiles.size(); i++) {
            ImageView tile = tiles.get(i);
            setTile(tile, gridState[i]);
        }
    }
}
