package com.dlfsystems.BoothClient.views

import android.content.Context
import androidx.appcompat.app.AlertDialog
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.dlfsystems.BoothClient.R
import com.dlfsystems.BoothClient.plusAssign
import com.dlfsystems.BoothClient.views
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.lang.Integer.max

class Tagbag @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ViewGroup(context, attrs, defStyle) {

    class TagView @JvmOverloads constructor (
        val tagString: String,
        sidePadding: Int,
        topBottomPadding: Int,
        backgroundResource: Int,
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
    ) : TextView(context, attrs, defStyle) {

        val click = PublishSubject.create<TagView>()

        init {
            text = tagString
            setPadding(sidePadding, topBottomPadding, sidePadding, topBottomPadding)
            setBackgroundResource(backgroundResource)
            setOnClickListener {
                click.onNext(it as TagView)
            }
        }
    }

    val styledAttrs = context.theme.obtainStyledAttributes(attrs, R.styleable.Tagbag, 0, 0)
    val tagSpacing = styledAttrs.getInteger(R.styleable.Tagbag_tagSpacing, 0)
    val rowSpacing = styledAttrs.getInteger(R.styleable.Tagbag_rowSpacing, 0)
    val description = styledAttrs.getString(R.styleable.Tagbag_description) ?: "tags"
    val newTagView = TagView("+", tagSpacing, rowSpacing, R.drawable.tagbag_add_background, context)
    val initialEditable = styledAttrs.getBoolean(R.styleable.Tagbag_editable, false)
    var editable: Boolean = initialEditable
    set(value) {
        if (value != field) {
            field = value
            makeTagViews()
            invalidate()
        }
    }

    private var disposables = CompositeDisposable()
    var addInputView = EditText(context)
    private val tagsSubject = PublishSubject.create<ArrayList<String>>()
    val tagStrings: Observable<ArrayList<String>> = tagsSubject
    private var tags: ArrayList<String> = ArrayList(0)

    init {
        disposables += newTagView.click.observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                requestTagToAdd()
            }
    }

    fun replaceTags(newTags: ArrayList<String>) {
        tags.clear()
        tags.addAll(newTags)
        makeTagViews()
        invalidate()
    }

    private var calcWidth = 0
    private var calcHeight = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)

        val maxWidth = if (widthMode == View.MeasureSpec.EXACTLY || widthMode == View.MeasureSpec.AT_MOST) widthSize else 999999
        val maxHeight = if (heightMode == View.MeasureSpec.EXACTLY || heightMode == View.MeasureSpec.AT_MOST) heightSize else 999999

        makeLayout(maxWidth, maxHeight, true, widthMeasureSpec, heightMeasureSpec)

        setMeasuredDimension(calcWidth, calcHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        makeLayout(r - l, b - t, false,
            MeasureSpec.makeMeasureSpec(r - l, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(b - t, MeasureSpec.AT_MOST))
    }

    fun makeLayout(maxWidth: Int, maxHeight: Int, measureOnly: Boolean, widthMeasureSpec: Int = 0, heightMeasureSpec: Int = 0) {
        var widthRemaining = maxWidth
        var heightRemaining = maxHeight
        var currentRowHeight = 0
        var finalWidth = 0

        fun nextRow() {
            heightRemaining -= currentRowHeight + rowSpacing
            finalWidth = max(finalWidth, maxWidth - widthRemaining)
            widthRemaining = maxWidth
        }

        tagViews().forEach {
            if (measureOnly) measureChild(it, widthMeasureSpec, heightMeasureSpec)
            else it.measure(widthMeasureSpec, heightMeasureSpec)

            currentRowHeight = max(currentRowHeight, it.measuredHeight)
            if (widthRemaining - (it.measuredWidth + tagSpacing) < 0) { nextRow() }

            if (heightRemaining >= 0) {
                if (!measureOnly) {
                    it.layout(
                        maxWidth - widthRemaining, maxHeight - heightRemaining,
                        (maxWidth - widthRemaining) + it.measuredWidth,
                        (maxHeight - heightRemaining) + it.measuredHeight
                    )
                }
            }

            widthRemaining -= it.measuredWidth + tagSpacing
        }
        nextRow()

        calcWidth = finalWidth
        calcHeight = if (heightRemaining > 0) maxHeight - heightRemaining else maxHeight
    }

    fun tagViews(): List<TagView> = views.filter { it is TagView } as List<TagView>

    fun makeTagViews() {
        removeView(newTagView)
        tags.forEach { tagString ->
            if (tagViews().none { it.tagString == tagString })
                addView(makeTagView(tagString))
        }
        tagViews().filter { !tags.contains(it.tagString) }.forEach {
            removeView(it) }
        if (editable) addView(newTagView)
    }

    fun makeTagView(tagString: String): TagView {
        val newView = TagView(tagString, tagSpacing, rowSpacing, R.drawable.tagbag_background, context)

        disposables += newView.click.observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                onTagClick(it)
            }
        return newView
    }

    fun removeTag(tagView: TagView) {
        tags.remove(tagView.tagString)
        onTagsChanged()
    }

    fun onTagClick(tagView: TagView) {
        if (editable) {
            if (tagView == newTagView) {
                requestTagToAdd()
            } else {
                AlertDialog.Builder(context, R.style.DialogStyle)
                    .setTitle(tagView.tagString)
                    .setMessage("Remove tag '" + tagView.tag + "' from " + description + "?")
                    .setPositiveButton("Remove") { _, _ ->
                        removeTag(tagView)
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                    }
                    .create().show()
            }
        }
    }

    fun requestTagToAdd() {
        addInputView = EditText(context)
        addInputView.setTextColor(ContextCompat.getColor(context, R.color.black))
        addInputView.inputType = InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE or InputType.TYPE_CLASS_TEXT
        AlertDialog.Builder(context, R.style.DialogStyle)
            .setTitle("Add tag to " + description)
            .setView(addInputView)
            .setPositiveButton("Add") { _, _ ->
                validateAndAddTag(addInputView.text.toString())
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .create().show()
    }

    fun validateAndAddTag(tag: String) {
        tags.add(tag)
        onTagsChanged()
    }

    fun onTagsChanged() {
        makeTagViews()
        invalidate()
        tagsSubject.onNext(tags.clone() as ArrayList<String>)
    }
}