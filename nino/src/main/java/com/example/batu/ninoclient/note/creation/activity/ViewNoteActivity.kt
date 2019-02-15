package com.example.batu.ninoclient.note.creation.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.View
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView
import com.github.bijoysingh.starter.recyclerview.MultiRecyclerViewControllerItem
import com.github.bijoysingh.starter.recyclerview.RecyclerViewBuilder
import com.example.batu.ninoclient.R
import com.example.batu.ninoclient.config.CoreConfig
import com.example.batu.ninoclient.config.CoreConfig.Companion.notesDb
import com.example.batu.ninoclient.core.format.Format
import com.example.batu.ninoclient.core.format.FormatBuilder
import com.example.batu.ninoclient.core.format.FormatType
import com.example.batu.ninoclient.core.note.NoteBuilder
import com.example.batu.ninoclient.core.note.NoteState
import com.example.batu.ninoclient.core.note.getFormats
import com.example.batu.ninoclient.core.note.isUnsaved
import com.example.batu.ninoclient.database.room.note.Note
import com.example.batu.ninoclient.note.*
import com.example.batu.ninoclient.note.actions.NoteOptionsBottomSheet
import com.example.batu.ninoclient.note.activity.INoteOptionSheetActivity
import com.example.batu.ninoclient.note.creation.specs.NoteViewBottomBar
import com.example.batu.ninoclient.note.creation.specs.NoteViewTopBar
import com.example.batu.ninoclient.note.formats.FormatAdapter
import com.example.batu.ninoclient.note.formats.IFormatRecyclerViewActivity
import com.example.batu.ninoclient.note.formats.getFormatControllerItems
import com.example.batu.ninoclient.note.formats.recycler.KEY_EDITABLE
import com.example.batu.ninoclient.note.formats.recycler.KEY_NOTE_COLOR
import com.example.batu.ninoclient.settings.sheet.NoteSettingsOptionsBottomSheet
import com.example.batu.ninoclient.settings.sheet.SettingsOptionsBottomSheet.Companion.KEY_MARKDOWN_ENABLED
import com.example.batu.ninoclient.settings.sheet.TextSizeBottomSheet
import com.example.batu.ninoclient.settings.sheet.UISettingsOptionsBottomSheet.Companion.useNoteColorAsBackground
import com.example.batu.ninoclient.support.specs.ToolbarColorConfig
import com.example.batu.ninoclient.support.ui.*
import com.example.batu.ninoclient.support.ui.ColorUtil.darkerColor
import com.example.batu.ninoclient.support.utils.bind
import kotlinx.android.synthetic.main.activity_advanced_note.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


const val INTENT_KEY_NOTE_ID = "NOTE_ID"
const val INTENT_KEY_DISTRACTION_FREE = "DISTRACTION_FREE"


data class NoteViewColorConfig(
    var backgroundColor: Int = Color.BLACK,
    var toolbarBackgroundColor: Int = Color.BLACK,
    var toolbarIconColor: Int = Color.BLACK,
    var statusBarColor: Int = Color.BLACK)

open class ViewAdvancedNoteActivity : ThemedActivity(), INoteOptionSheetActivity, IFormatRecyclerViewActivity {

  var focusedFormat: Format? = null
  protected var note: Note? = null

  protected lateinit var context: Context
  protected lateinit var adapter: FormatAdapter
  protected lateinit var formats: MutableList<Format>
  protected lateinit var formatsView: RecyclerView
  protected var isDistractionFree: Boolean = false

  val creationFinished = AtomicBoolean(false)
  val colorConfig = NoteViewColorConfig()
  var lastKnownNoteColor = 0

  val rootView: View by bind(R.id.root_layout)

  protected open val editModeValue: Boolean
    get() = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_advanced_note)
    context = this
    isDistractionFree = intent.getBooleanExtra(INTENT_KEY_DISTRACTION_FREE, false)

    setRecyclerView()

    GlobalScope.launch(Dispatchers.IO) {
      var noteId = intent.getIntExtra(INTENT_KEY_NOTE_ID, 0)
      if (noteId == 0 && savedInstanceState != null) {
        noteId = savedInstanceState.getInt(INTENT_KEY_NOTE_ID, 0)
      }
      if (noteId != 0) {
        note = notesDb.getByID(noteId)
      }
      if (note === null) {
        note = NoteBuilder().emptyNote(NoteSettingsOptionsBottomSheet.genDefaultColor())
      }
      GlobalScope.launch(Dispatchers.Main) {
        setToolbars()
        setEditMode()
        notifyThemeChange()
        onCreationFinished()
      }
      creationFinished.set(true)
    }
  }

  override fun onResume() {
    super.onResume()
    CoreConfig.instance.startListener(this)

    if (!creationFinished.get()) {
      return
    }
    onResumeAction()
    notifyThemeChange()
  }

  protected open fun onCreationFinished() {

  }

  protected open fun onResumeAction() {
    GlobalScope.launch(Dispatchers.IO) {
      note = notesDb.getByID(intent.getIntExtra(INTENT_KEY_NOTE_ID, 0))
      when {
        note == null -> finish()
        else -> GlobalScope.launch(Dispatchers.Main) { setNote() }
      }
    }
  }

  protected open fun setEditMode() {
    setEditMode(editModeValue)
    formatsView.setBackgroundColor(CoreConfig.instance.themeController().get(ThemeColorType.BACKGROUND))
  }

  protected fun setEditMode(mode: Boolean) {
    resetBundle()
    setNote()
  }

  private fun startDistractionFreeMode() {
    var uiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        or View.SYSTEM_UI_FLAG_FULLSCREEN)
    if (Build.VERSION.SDK_INT >= 19) {
      uiVisibility = (uiVisibility or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }
    window.decorView.systemUiVisibility = uiVisibility
  }

  private fun resetBundle() {
    val bundle = Bundle()
    bundle.putBoolean(KEY_EDITABLE, editModeValue)
    bundle.putBoolean(KEY_MARKDOWN_ENABLED, CoreConfig.instance.store().get(KEY_MARKDOWN_ENABLED, true))
    bundle.putBoolean(KEY_NIGHT_THEME, CoreConfig.instance.themeController().isNightTheme())
    bundle.putInt(TextSizeBottomSheet.KEY_TEXT_SIZE, TextSizeBottomSheet.getDefaultTextSize())
    bundle.putInt(KEY_NOTE_COLOR, note!!.color)
    bundle.putString(INTENT_KEY_NOTE_ID, note!!.uuid)
    adapter.setExtra(bundle)
  }

  protected open fun setNote() {
    val currentNote = note
    if (currentNote === null) {
      return
    }

    setNoteColor(currentNote.color)
    adapter.clearItems()

    if (isDistractionFree) {
      adapter.addItem(Format(FormatType.SEPARATOR))
    }

    formats = when (editModeValue) {
      true -> currentNote.getFormats()
      false -> currentNote.getSmartFormats()
    }.toMutableList()
    adapter.addItems(formats)

    if (!editModeValue) {
      maybeAddTags()
      maybeAddEmptySpace()
    }
  }

  private fun maybeAddTags() {
    val currentNote = note
    if (currentNote === null) {
      return
    }

    val tagLabel = currentNote.getTagString()
    if (tagLabel.isEmpty()) {
      return
    }

    val format = Format(FormatType.TAG, tagLabel)
    adapter.addItem(format)
  }

  private fun maybeAddEmptySpace() {
    adapter.addItem(Format(FormatType.EMPTY))
  }

  private fun setRecyclerView() {
    adapter = FormatAdapter(this)
    formatsView = RecyclerViewBuilder(this)
        .setAdapter(adapter)
        .setView(this, R.id.advanced_note_recycler)
        .build()
  }

  open fun setFormat(format: Format) {
    // do nothing
  }

  open fun createOrChangeToNextFormat(format: Format) {
    // do nothing
  }

  open fun setFormatChecked(format: Format, checked: Boolean) {
    val position = getFormatIndex(format)
    if (position == -1) {
      return
    }
    format.formatType = if (checked) FormatType.CHECKLIST_CHECKED else FormatType.CHECKLIST_UNCHECKED
    formats[position] = format
    adapter.updateItem(format, position)
    updateNoteForChecked()
  }

  private fun setToolbars() {
    val currentNote = note
    if (currentNote === null) {
      return
    }

    setBottomToolbar()
    setTopToolbar()
    notifyToolbarColor()

    if (isDistractionFree) {
      startDistractionFreeMode()
    }
  }

  fun openMoreOptions() {
    NoteOptionsBottomSheet.openSheet(this@ViewAdvancedNoteActivity, note!!)
  }

  fun openEditor() {
    note!!.openEdit(context)
  }

  protected open fun notifyToolbarColor() {
    val currentNote = note
    if (currentNote === null) {
      return
    }

    val theme = CoreConfig.instance.themeController()
    when {
      !useNoteColorAsBackground -> {
        colorConfig.backgroundColor = theme.get(ThemeColorType.BACKGROUND)
        colorConfig.toolbarIconColor = theme.get(ThemeColorType.TOOLBAR_ICON)
        colorConfig.statusBarColor = colorConfig.backgroundColor
        colorConfig.toolbarBackgroundColor = theme.get(ThemeColorType.TOOLBAR_BACKGROUND)
      }
      ColorUtil.isLightColored(currentNote.color) -> {
        colorConfig.backgroundColor = currentNote.color
        colorConfig.toolbarIconColor = theme.get(context, Theme.LIGHT, ThemeColorType.TOOLBAR_ICON)
        colorConfig.statusBarColor = darkerColor(currentNote.color)
        colorConfig.toolbarBackgroundColor = colorConfig.statusBarColor
      }
      else -> {
        colorConfig.backgroundColor = currentNote.color
        colorConfig.toolbarIconColor = theme.get(context, Theme.DARK, ThemeColorType.TOOLBAR_ICON)
        colorConfig.statusBarColor = darkerColor(currentNote.color)
        colorConfig.toolbarBackgroundColor = colorConfig.statusBarColor
      }
    }

    setSystemTheme(colorConfig.statusBarColor)
    rootView.setBackgroundColor(colorConfig.backgroundColor)
    formatsView.setBackgroundColor(colorConfig.backgroundColor)

    resetBundle()
    adapter.notifyDataSetChanged()
  }

  protected open fun setBottomToolbar() {
    lithoBottomToolbar.removeAllViews()
    val componentContext = ComponentContext(this)
    lithoBottomToolbar.addView(
        LithoView.create(componentContext,
            NoteViewBottomBar.create(componentContext)
                .colorConfig(ToolbarColorConfig(colorConfig.toolbarBackgroundColor, colorConfig.toolbarIconColor))
                .build()))
  }

  protected open fun setTopToolbar() {
    lithoTopToolbar.removeAllViews()
    val componentContext = ComponentContext(this)
    lithoTopToolbar.addView(
        LithoView.create(componentContext,
            NoteViewTopBar.create(componentContext).build()))
  }

  protected open fun setNoteColor(color: Int) {

  }

  protected fun maybeSaveNote(sync: Boolean) {
    if (note!!.getFormats().isEmpty() && note!!.isUnsaved()) {
      return
    }
    note!!.updateTimestamp = Calendar.getInstance().timeInMillis
    when (sync) {
      true -> note!!.save(context)
      false -> note!!.saveWithoutSync(context)
    }
  }

  private fun updateNoteForChecked() {
    note!!.description = FormatBuilder().getDescription(formats.sorted())
    setNote()
    maybeSaveNote(true)
  }

  fun notifyNoteChange() {
    notifyToolbarColor()
  }

  protected fun getFormatIndex(format: Format): Int = getFormatIndex(format.uid)

  protected fun getFormatIndex(formatUid: Int): Int {
    var position = 0
    for (fmt in formats) {
      if (fmt.uid == formatUid) {
        return position
      }
      position++
    }
    return -1
  }

  override fun notifyThemeChange() {
    notifyToolbarColor()
  }

  public override fun onSaveInstanceState(savedInstanceState: Bundle?) {
    super.onSaveInstanceState(savedInstanceState)
    if (savedInstanceState == null) {
      return
    }
    savedInstanceState.putInt(INTENT_KEY_NOTE_ID, if (note == null || note!!.uid == null) 0 else note!!.uid)
  }

  fun note() = note!!

  companion object {
    const val HANDLER_UPDATE_TIME = 1000

    fun getIntent(context: Context, note: Note): Intent {
      val intent = Intent(context, ViewAdvancedNoteActivity::class.java)
      intent.putExtra(INTENT_KEY_NOTE_ID, note.uid)
      return intent
    }
  }

  /**
   * Start : INoteOptionSheetActivity Functions
   */

  override fun updateNote(note: Note) {
    note.save(this)
    notifyNoteChange()
  }

  override fun markItem(note: Note, state: NoteState) {
    note.mark(this, state)
  }

  override fun moveItemToTrashOrDelete(note: Note) {
    note.softDelete(context)
    finish()
  }

  override fun notifyTagsChanged(note: Note) {
    setNote()
  }

  override fun getSelectMode(note: Note): String {
    return NoteState.DEFAULT.name
  }

  override fun notifyResetOrDismiss() {
    finish()
  }

  override fun lockedContentIsHidden() = false

  /**
   * End : INoteOptionSheetActivity
   */


  /**
   * Start : IFormatRecyclerView Functions
   */

  override fun context(): Context {
    return this
  }

  override fun controllerItems(): List<MultiRecyclerViewControllerItem<Format>> {
    return getFormatControllerItems()
  }

  override fun deleteFormat(format: Format) {
    // do nothing
  }

  override fun moveFormat(fromPosition: Int, toPosition: Int) {
    // do nothing
  }

  /**
   * End : IFormatRecyclerView
   */

}