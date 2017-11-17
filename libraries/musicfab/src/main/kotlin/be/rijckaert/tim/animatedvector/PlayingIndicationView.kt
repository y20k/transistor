package be.rijckaert.tim.animatedvector

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.widget.ImageView
//import be.rijckaert.tim.vector.sample.library.R

class PlayingIndicationView : ImageView {

    private val playAnimation: Drawable by lazy { ContextCompat.getDrawable(context, R.drawable.avd_playing_identifier) }
    private val maximumAnimationDuration by lazy { getMaxAnimationDuration() }
    private val animationResetFunction = { resetAnimation() }
    private fun getMaxAnimationDuration(): Long {
        with(context.resources) {
            val max = intArrayOf(
                    getInteger(R.integer.animation_wave_1_part_1) + getInteger(R.integer.animation_wave_1_part_2),
                    getInteger(R.integer.animation_wave_2_part_1) + getInteger(R.integer.animation_wave_2_part_2),
                    getInteger(R.integer.animation_wave_3_part_1) + getInteger(R.integer.animation_wave_3_part_2),
                    getInteger(R.integer.animation_wave_4_part_1) + getInteger(R.integer.animation_wave_4_part_2)
            ).max()
            return (max ?: 0).toLong()
        }
    }

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        this.setImageDrawable(playAnimation)
        resetAnimation()
    }

    private fun resetAnimation() {
        if (playAnimation is Animatable) {
            val vectorDrawableCompat = playAnimation as Animatable
            vectorDrawableCompat.start()

            //Which one?
            //this.postDelayed(animationResetFunction, maximumAnimationDuration)
            this.postOnAnimation(animationResetFunction)
        }
    }
}