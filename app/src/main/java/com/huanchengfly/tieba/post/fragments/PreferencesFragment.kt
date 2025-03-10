package com.huanchengfly.tieba.post.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.google.android.material.snackbar.Snackbar
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.DataStorePreference
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.BlockListActivity
import com.huanchengfly.tieba.post.activities.LoginActivity
import com.huanchengfly.tieba.post.components.prefs.TimePickerPreference
import com.huanchengfly.tieba.post.fragments.preference.PreferencesFragment
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.utils.*

class PreferencesFragment : PreferencesFragment() {
    private var loginInfo: Account? = null
    override fun onResume() {
        super.onResume()
        refresh()
    }

    //忽略电池优化
    private fun ignoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = attachContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(attachContext.packageName)) {
                val intent = Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:${attachContext.packageName}")
                startActivity(intent)
            }
        }
    }

    private fun refresh() {
        val experimentalFeatures =
            attachContext.resources.getStringArray(R.array.experimental_features)
        experimentalFeatures.forEach {
            findPreference<Preference>(it)?.isVisible =
                attachContext.appPreferences.showExperimentalFeatures
        }
        loginInfo = AccountUtil.getLoginInfo()
        val accounts = AccountUtil.allAccounts
        val usernameList: MutableList<String> = ArrayList()
        val idList: MutableList<String> = ArrayList()
        for (account in accounts) {
            usernameList.add(account.nameShow!!)
            idList.add(account.id.toString())
        }
        val accountsPreference = findPreference<ListPreference>("switch_account")
        accountsPreference!!.entries = usernameList.toTypedArray()
        accountsPreference.entryValues = idList.toTypedArray()
        if (loginInfo != null) {
            accountsPreference.value = loginInfo!!.id.toString()
            accountsPreference.summary = "已登录账号 " + loginInfo!!.nameShow
        } else {
            accountsPreference.summary = "未登录"
        }
    }

    @SuppressLint("ApplySharedPref")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        //preferenceManager.sharedPreferencesName = "settings"
        preferenceManager.preferenceDataStore = DataStorePreference()
        addPreferencesFromResource(R.xml.preferences)
        val accountsPreference = findPreference<ListPreference>("switch_account")
        accountsPreference!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                if (AccountUtil.switchAccount(
                        attachContext,
                        Integer.valueOf((newValue as String?)!!)
                    )
                ) {
                    refresh()
                    Toast.makeText(attachContext, R.string.toast_switch_success, Toast.LENGTH_SHORT)
                        .show()
                }
                false
            }
        findPreference<Preference>("ignore_battery_optimization")?.let {
            val powerManager = attachContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!powerManager.isIgnoringBatteryOptimizations(attachContext.packageName)) {
                        ignoreBatteryOptimization()
                    } else {
                        attachContext.toastShort(R.string.toast_ignore_battery_optimization_already)
                    }
                }
                true
            }
            it.isEnabled =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !powerManager.isIgnoringBatteryOptimizations(
                    attachContext.packageName
                )
            it.setSummaryProvider {
                when {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> {
                        attachContext.getString(R.string.summary_battery_optimization_old_android_version)
                    }
                    powerManager.isIgnoringBatteryOptimizations(attachContext.packageName) -> {
                        attachContext.getString(R.string.summary_battery_optimization_ignored)
                    }
                    else -> {
                        attachContext.getString(R.string.summary_ignore_battery_optimization)
                    }
                }
            }
        }
        findPreference<Preference>("copy_bduss")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val account = AccountUtil.getLoginInfo()
                if (account != null) {
                    TiebaUtil.copyText(attachContext, account.bduss, isSensitive = true)
                }
                true
            }
        findPreference<Preference>("exit_account")!!.isEnabled =
            AccountUtil.isLoggedIn()
        findPreference<Preference>("exit_account")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                DialogUtil.build(attachContext)
                    .setMessage(R.string.title_dialog_exit_account)
                    .setPositiveButton(R.string.button_sure_default) { _: DialogInterface?, _: Int ->
                        AccountUtil.exit(attachContext)
                        refresh()
                        if (AccountUtil.getLoginInfo() == null) {
                            attachContext.startActivity(
                                Intent(
                                    attachContext,
                                    LoginActivity::class.java
                                )
                            )
                        }
                    }
                    .setNegativeButton(R.string.button_cancel, null)
                    .create()
                    .show()
                true
            }
        findPreference<Preference>("black_list")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                startActivity(
                    Intent(
                        attachContext,
                        BlockListActivity::class.java
                    ).putExtra("category", Block.CATEGORY_BLACK_LIST)
                )
                true
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            findPreference<Preference>("follow_system_night")!!.isEnabled = true
            findPreference<Preference>("follow_system_night")!!.summary = null
        } else {
            findPreference<Preference>("follow_system_night")!!.isEnabled = false
            findPreference<Preference>("follow_system_night")!!.setSummary(R.string.summary_follow_system_night_disabled)
        }
        findPreference<Preference>("show_top_forum_in_normal_list")!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference, _: Any? ->
                preference.setSummary(R.string.summary_show_top_forum_in_normal_list_changed)
                true
            }
        findPreference<Preference>("status_bar_darker")!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference, _: Any? ->
                preference.setSummary(R.string.summary_status_bar_darker_changed)
                true
            }
        findPreference<Preference>("hideExplore")!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference, _: Any? ->
                preference.setSummary(R.string.summary_change_need_restart)
                true
            }
        findPreference<Preference>("white_list")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                startActivity(
                    Intent(
                        attachContext,
                        BlockListActivity::class.java
                    ).putExtra("category", Block.CATEGORY_WHITE_LIST)
                )
                true
            }
        val timePickerPreference = findPreference<TimePickerPreference>("auto_sign_time")
        timePickerPreference!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference, newValue: Any? ->
                preference.summary =
                    attachContext.getString(R.string.summary_auto_sign_time, newValue as String?)
                true
            }
        timePickerPreference.summary = attachContext.getString(
            R.string.summary_auto_sign_time,
            preferenceManager.preferenceDataStore!!.getString("auto_sign_time", "09:00")
        )
        val clearCache = findPreference<Preference>("clear_cache")
        clearCache!!.summary = attachContext.getString(
            R.string.tip_cache,
            ImageCacheUtil.getCacheSize(attachContext)
        )
        clearCache.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference ->
                ImageCacheUtil.clearImageAllCache(attachContext)
                if (view != null) Util.createSnackbar(
                    requireView(),
                    R.string.toast_clear_picture_cache_success,
                    Snackbar.LENGTH_SHORT
                ).show()
                preference.summary = attachContext.getString(R.string.tip_cache, "0.0B")
                true
            }
        val littleTaliPreference = findPreference<EditTextPreference>("little_tail")
        val littleTali = preferenceManager.preferenceDataStore!!.getString("little_tail", "")
        if (littleTali!!.isEmpty()) {
            littleTaliPreference!!.setSummary(R.string.tip_no_little_tail)
        } else {
            littleTaliPreference!!.summary = littleTali
            littleTaliPreference.text = littleTali
        }
        littleTaliPreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, value: Any? ->
                if (value is String) {
                    if (value.isEmpty()) {
                        littleTaliPreference.setSummary(R.string.tip_no_little_tail)
                    } else {
                        littleTaliPreference.summary = value
                        littleTaliPreference.text = value
                    }
                }
                true
            }
        val aboutPreference = findPreference<Preference>("about")
        val useCustomTabs = findPreference<SwitchPreference>("use_custom_tabs")
        useCustomTabs!!.isEnabled =
            !preferenceManager.preferenceDataStore!!.getBoolean("use_webview", true)
        findPreference<Preference>("use_webview")!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                useCustomTabs.isEnabled = !(newValue as Boolean)
                true
            }
        initListPreference("dark_theme", "dark")
        aboutPreference!!.summary =
            getString(R.string.tip_about, BuildConfig.VERSION_NAME)
        refresh()
        findPreference<SwitchPreference>("enableNewUi")?.setOnPreferenceChangeListener { _, newValue ->
            App.INSTANCE.setIcon(newValue == true)
            true
        }
        /*
        try {
            val preferenceGroupClazz = PreferenceGroup::class.java
            val preferencesField = preferenceGroupClazz.getDeclaredField("mPreferences")
            preferencesField.isAccessible = true
            val preferencesList = preferencesField.get(preferenceScreen) as List<Preference>
            attachContext.toastShort("${preferencesList.size}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        */
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDivider(
            ThemeUtils.tintDrawable(
                ContextCompat.getDrawable(attachContext, R.drawable.drawable_divider_8dp),
                ThemeUtils.getColorById(attachContext, R.color.default_color_window_background)
            )
        )
        setDividerHeight(0)
    }

    private fun initSwitchPreference(key: String, defValue: Boolean = false) {
        val switchPreference = findPreference<SwitchPreference>(key)
        initSwitchPreference(switchPreference, defValue)
    }

    private fun initSwitchPreference(
        switchPreference: SwitchPreference?,
        defValue: Boolean = false
    ) {
        val value =
            preferenceManager.preferenceDataStore!!.getBoolean(switchPreference!!.key, defValue)
        switchPreference.isChecked = value
    }

    private fun initListPreference(key: String, defValue: String) {
        val listPreference = findPreference<ListPreference>(key)
        val value = preferenceManager.preferenceDataStore!!.getString(key, defValue)
        listPreference!!.value = value
    }

    companion object {
        const val TAG = "PreferencesFragment"
    }
}
