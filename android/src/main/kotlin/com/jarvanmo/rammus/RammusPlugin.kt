package com.jarvanmo.rammus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.util.Log
import com.alibaba.sdk.android.push.CommonCallback
import com.alibaba.sdk.android.push.huawei.HuaWeiRegister
import com.alibaba.sdk.android.push.noonesdk.PushServiceFactory
import com.alibaba.sdk.android.push.register.*
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar


class RammusPlugin(private val registrar: Registrar, private val methodChannel: MethodChannel) : MethodCallHandler {
    companion object {
        private const val TAG = "RammusPlugin"
        private val inHandler = Handler()
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "com.jarvanmo/rammus")
            RammusPushHandler.methodChannel = channel
            channel.setMethodCallHandler(RammusPlugin(registrar, channel))
        }
        @JvmStatic
        fun initPushService(application: Application){
            PushServiceFactory.init(application.applicationContext)
            val pushService = PushServiceFactory.getCloudPushService()
            pushService.register(application.applicationContext, object : CommonCallback {
                override fun onSuccess(response: String?) {
                    inHandler.postDelayed({
                        RammusPushHandler.methodChannel?.invokeMethod("initCloudChannelResult", mapOf(
                                "isSuccessful" to true,
                                "response" to response
                        ))
                    }, 2000)
                }

                override fun onFailed(errorCode: String?, errorMessage: String?) {
                    inHandler.postDelayed({
                        RammusPushHandler.methodChannel?.invokeMethod("initCloudChannelResult", mapOf(
                                "isSuccessful" to false,
                                "errorCode" to errorCode,
                                "errorMessage" to errorMessage
                        ))
                    }, 2000)
                }
            })
            pushService.setPushIntentService(RammusPushIntentService::class.java)
            val appInfo = application.packageManager
                    .getApplicationInfo(application.packageName, PackageManager.GET_META_DATA)
            val xiaomiAppId = appInfo.metaData.getString("com.xiaomi.push.client.app_id")
            val xiaomiAppKey = appInfo.metaData.getString("com.xiaomi.push.client.app_key")
            if ((xiaomiAppId != null && xiaomiAppId.isNotBlank())
                    && (xiaomiAppKey != null && xiaomiAppKey.isNotBlank())){
                Log.d(TAG, "正在注册小米推送服务...")
                MiPushRegister.register(application.applicationContext, xiaomiAppId, xiaomiAppKey)
            }
            val huaweiAppId = appInfo.metaData.getString("com.huawei.hms.client.appid")
            if (huaweiAppId != null && huaweiAppId.toString().isNotBlank()){
                Log.d(TAG, "正在注册华为推送服务...")
                HuaWeiRegister.register(application)
            }
            val oppoAppKey = appInfo.metaData.getString("com.oppo.push.client.app_key")
            val oppoAppSecret = appInfo.metaData.getString("com.oppo.push.client.app_secret")
            if ((oppoAppKey != null && oppoAppKey.isNotBlank())
                    && (oppoAppSecret != null && oppoAppSecret.isNotBlank())){
                Log.d(TAG, "正在注册Oppo推送服务...")
                OppoRegister.register(application.applicationContext, oppoAppKey, oppoAppSecret)
            }
            val meizuAppId = appInfo.metaData.getString("com.meizu.push.client.app_id")
            val meizuAppKey = appInfo.metaData.getString("com.meizu.push.client.app_key")
            if ((meizuAppId != null && meizuAppId.isNotBlank())
                    && (meizuAppKey != null && meizuAppKey.isNotBlank())){
                Log.d(TAG, "正在注册魅族推送服务...")
                MeizuRegister.register(application.applicationContext, meizuAppId, meizuAppKey)
            }
            val vivoAppId = appInfo.metaData.getString("com.vivo.push.app_id")
            val vivoApiKey = appInfo.metaData.getString("com.vivo.push.api_key")
            if ((vivoAppId != null && vivoAppId.isNotBlank())
                    && (vivoApiKey != null && vivoApiKey.isNotBlank())){
                Log.d(TAG, "正在注册Vivo推送服务...")
                VivoRegister.register(application.applicationContext)
            }
            val gcmSendId = appInfo.metaData.getString("com.gcm.push.send_id")
            val gcmApplicationId = appInfo.metaData.getString("com.gcm.push.app_id")
            if ((gcmSendId != null && gcmSendId.isNotBlank())
                    && (gcmApplicationId != null && gcmApplicationId.isNotBlank())){
                Log.d(TAG, "正在注册Gcm推送服务...")
                GcmRegister.register(application.applicationContext, gcmSendId, gcmApplicationId)
            }
        }


        /// 崩溃分析
        @JvmStatic
        fun initApmCrashService(application: Application) {

            val appInfo = application.packageManager
                    .getApplicationInfo(application.packageName, PackageManager.GET_META_DATA)

            val aliAppKey = appInfo.metaData.getString("com.xiaomi.push.client.app_key")
            val aliAppVersion = appInfo.metaData.getString("com.xiaomi.push.client.app_version")
            val aliAppSecret = appInfo.metaData.getString("com.xiaomi.push.client.app_secret")
            val aliChannel = appInfo.metaData.getString("com.xiaomi.push.client.app_channel")
            val aliUserNick = appInfo.metaData.getString("com.xiaomi.push.client.app_user_nick")
            val aliRsaPublicKey = appInfo.metaData.getString("com.xiaomi.push.client.app_rsa_public_key")


            val config = AliHaConfig()
            config.appKey = aliAppKey
            config.appVersion = aliAppVersion
            config.appSecret = aliAppSecret
            config.channel = aliChannel
            config.userNick = aliUserNick
            config.application = this
            config.context = getApplicationContext()
            config.isAliyunos = false
            config.rsaPublicKey = aliRsaPublicKey //配置项

            //启动CrashReporter
            AliHaAdapter.getInstance().addPlugin(Plugin.crashreporter) /// 崩溃分析
            AliHaAdapter.getInstance().addPlugin(Plugin.apm) /// 性能分析
            AliHaAdapter.getInstance().start(config)
        }


        /// 移动数据分析
        @JvmStatic
        fun initAnalysisService() {
            /* 【注意】建议您在Application中初始化MAN，以保证正常获取MANService*/
            // 获取MAN服务
            val manService: MANService = MANServiceProvider.getService()
            // 打开调试日志，线上版本建议关闭
            // manService.getMANAnalytics().turnOnDebug();
            // 若需要关闭 SDK 的自动异常捕获功能可进行如下操作(如需关闭crash report，建议在init方法调用前关闭crash),详见文档5.4
            manService.getMANAnalytics().turnOffCrashReporter()
            // 设置渠道（用以标记该app的分发渠道名称），如果不关心可以不设置即不调用该接口，渠道设置将影响控制台【渠道分析】栏目的报表展现。如果文档3.3章节更能满足您渠道配置的需求，就不要调用此方法，按照3.3进行配置即可；1.1.6版本及之后的版本，请在init方法之前调用此方法设置channel.
            manService.getMANAnalytics().setChannel("某渠道")
            // MAN初始化方法之一，从AndroidManifest.xml中获取appKey和appSecret初始化，若您采用上述 2.3中"统一接入的方式"，则使用当前init方法即可。
            manService.getMANAnalytics().init(this, getApplicationContext())
            // MAN另一初始化方法，手动指定appKey和appSecret
            // 若您采用上述2.3中"统一接入的方式"，则无需使用当前init方法。
            // String appKey = "******";
            // String appSecret = "******";
            // manService.getMANAnalytics().init(this, getApplicationContext(), appKey, appSecret);
            // 通过此接口关闭页面自动打点功能，详见文档4.2
            manService.getMANAnalytics().turnOffAutoPageTrack()
            // 若AndroidManifest.xml 中的 android:versionName 不能满足需求，可在此指定
            // 若在上述两个地方均没有设置appversion，上报的字段默认为null
            manService.getMANAnalytics().setAppVersion("3.1.1")
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "deviceId" -> result.success(PushServiceFactory.getCloudPushService().deviceId)
            "turnOnPushChannel" -> turnOnPushChannel(result)
            "turnOffPushChannel" -> turnOffPushChannel(result)
            "checkPushChannelStatus" -> checkPushChannelStatus(result)
            "bindAccount" -> bindAccount(call, result)
            "unbindAccount" -> unbindAccount(result)
            "bindTag" -> bindTag(call, result)
            "unbindTag" -> unbindTag(call, result)
            "listTags" -> listTags(call, result)
            "addAlias" -> addAlias(call, result)
            "removeAlias" -> removeAlias(call, result)
            "listAliases" -> listAliases(result)
            "setupNotificationManager" -> setupNotificationManager(call, result)
            "bindPhoneNumber" -> bindPhoneNumber(call, result)
            "unbindPhoneNumber" -> unbindPhoneNumber(result)
            else -> result.notImplemented()
        }

    }

    /// 推送
    private fun turnOnPushChannel(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.turnOnPushChannel(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    private fun turnOffPushChannel(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.turnOffPushChannel(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    private fun checkPushChannelStatus(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.checkPushChannelStatus(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    private fun bindAccount(call: MethodCall, result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.bindAccount(call.arguments as String?, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    private fun unbindAccount(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.unbindAccount(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    //bindPhoneNumber
    private fun bindPhoneNumber(call: MethodCall, result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.bindPhoneNumber(call.arguments as String?, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    private fun unbindPhoneNumber(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.unbindPhoneNumber(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    private fun bindTag(call: MethodCall, result: Result) {
//        target: Int, tags: Array<String>, alias: String, callback: CommonCallback
        val target = call.argument("target") ?: 1
        val tagsInArrayList = call.argument("tags") ?: arrayListOf<String>()
        val alias = call.argument<String?>("alias")

        val arr = arrayOfNulls<String>(tagsInArrayList.size)
        val tags: Array<String> = tagsInArrayList.toArray(arr)

        val pushService = PushServiceFactory.getCloudPushService()

        pushService.bindTag(target, tags, alias, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    private fun unbindTag(call: MethodCall, result: Result) {
//        target: Int, tags: Array<String>, alias: String, callback: CommonCallback
        val target = call.argument("target") ?: 1
        val tagsInArrayList = call.argument("tags") ?: arrayListOf<String>()
        val alias = call.argument<String?>("alias")

        val arr = arrayOfNulls<String>(tagsInArrayList.size)
        val tags: Array<String> = tagsInArrayList.toArray(arr)

        val pushService = PushServiceFactory.getCloudPushService()

        pushService.unbindTag(target, tags, alias, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    private fun listTags(call: MethodCall, result: Result) {
        val target = call.arguments as Int? ?: 1
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.listTags(target, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    private fun addAlias(call: MethodCall, result: Result) {
        val alias = call.arguments as String?
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.addAlias(alias, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    private fun removeAlias(call: MethodCall, result: Result) {
        val alias = call.arguments as String?
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.removeAlias(alias, object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    private fun listAliases(result: Result) {
        val pushService = PushServiceFactory.getCloudPushService()
        pushService.listAliases(object : CommonCallback {
            override fun onSuccess(response: String?) {
                result.success(mapOf(
                        "isSuccessful" to true,
                        "response" to response
                ))

            }

            override fun onFailed(errorCode: String?, errorMessage: String?) {
                result.success(mapOf(
                        "isSuccessful" to false,
                        "errorCode" to errorCode,
                        "errorMessage" to errorMessage
                ))
            }
        })
    }

    private fun setupNotificationManager(call: MethodCall, result: Result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = call.arguments as List<Map<String, Any?>>
            val mNotificationManager = registrar.context().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannels = mutableListOf<NotificationChannel>()
            for (channel in channels){
                // 通知渠道的id
                val id = channel["id"] ?: registrar.context().packageName
                // 用户可以看到的通知渠道的名字.
                val name = channel["name"] ?: registrar.context().packageName
                // 用户可以看到的通知渠道的描述
                val description = channel["description"] ?: registrar.context().packageName
                val importance = channel["importance"] ?: NotificationManager.IMPORTANCE_DEFAULT
                val mChannel = NotificationChannel(id as String, name as String, importance as Int)
                // 配置通知渠道的属性
                mChannel.description = description as String
                mChannel.enableLights(true)
                mChannel.enableVibration(true)
                notificationChannels.add(mChannel)
            }
            if (notificationChannels.isNotEmpty()){
                mNotificationManager.createNotificationChannels(notificationChannels)
            }
        }
        result.success(true)
    }





}
