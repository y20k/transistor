package be.rijckaert.tim.animatedvector

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.support.design.widget.FloatingActionButton
import android.util.AttributeSet
import android.view.View


class FloatingMusicActionButton : FloatingActionButton {

    private val playToPauseDrawable: Drawable by lazy { context.resources.getDrawable(R.drawable.play_to_pause_animation, context.theme) }
    private val pauseToPlayDrawable: Drawable by lazy { context.resources.getDrawable(R.drawable.pause_to_play_animation, context.theme) }
    private val playToStopDrawable: Drawable by lazy { context.resources.getDrawable(R.drawable.play_to_stop_animation, context.theme) }
    private val stopToPlayDrawable: Drawable by lazy { context.resources.getDrawable(R.drawable.stop_to_play_animation, context.theme) }
    private val maximumAnimationDuration by lazy { context.resources.getInteger(R.integer.play_button_animation_duration).toLong() }

    private var listener: OnMusicFabClickListener? = null

    var currentMode: Mode
        private set
    val currentDrawable: Drawable
        get() {
            return getAnimationDrawable()
        }

    //<editor-fold desc="Chaining Constructors">
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        val typedArray = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.FloatingMusicActionButton,
                0, 0)

        try {
            currentMode = getMode(typedArray.getInteger(R.styleable.FloatingMusicActionButton_mode, 0))
        } finally {
            typedArray.recycle()
        }

        this.setOnClickListener {
            playAnimation()
            listener?.onClick(this)
        }

        this.setImageDrawable(currentDrawable)
    }
    //</editor-fold>

    fun playAnimation() {
        this.setImageDrawable(currentDrawable)
        currentDrawable.startAsAnimatable {
            setOppositeMode()
        }
    }

    private fun setOppositeMode() {
        currentMode = getOppositeMode()
    }

    @Synchronized
    fun changeMode(mode: Mode) {
        if (mode != currentMode) {
            currentMode = mode

            setOppositeMode()
            playAnimation()
        }
    }

    //<editor-fold desc="Helpers">
    private fun getMode(mode: Int): Mode = when (mode) {
        0 -> Mode.PLAY_TO_PAUSE
        1 -> Mode.PLAY_TO_STOP
        2 -> Mode.PAUSE_TO_PLAY
        3 -> Mode.STOP_TO_PLAY
        else -> Mode.STOP_TO_PLAY
    }

    fun getOppositeMode(): Mode {
        val position = if (currentMode.ordinal % 2 == 0) currentMode.ordinal + 1 else currentMode.ordinal - 1
        return Mode.values()[position]
    }

    private fun getAnimationDrawable(): Drawable =
            when (currentMode) {
                Mode.PLAY_TO_PAUSE -> {
                    playToPauseDrawable
                }
                Mode.PAUSE_TO_PLAY -> {
                    pauseToPlayDrawable
                }
                Mode.PLAY_TO_STOP -> {
                    playToStopDrawable
                }
                else -> {
                    stopToPlayDrawable
                }
            }

    fun setOnMusicFabClickListener(listener: OnMusicFabClickListener) {
        this.listener = listener
    }

    enum class Mode(private val modeInt: Int, val isShowingPlayIcon: Boolean = false) {
        PLAY_TO_PAUSE(0, true),
        PAUSE_TO_PLAY(2),
        PLAY_TO_STOP(1, true),
        STOP_TO_PLAY(3)
    }

    private fun Drawable.startAsAnimatable(finally: () -> Unit = { }) {
        if (this is Animatable) {
            this.start()
            this@FloatingMusicActionButton.postDelayed({
                finally.invoke()
            }, maximumAnimationDuration)
        }
    }

    interface OnMusicFabClickListener {
        fun onClick(view: View)
    }
    //</editor-fold>
}