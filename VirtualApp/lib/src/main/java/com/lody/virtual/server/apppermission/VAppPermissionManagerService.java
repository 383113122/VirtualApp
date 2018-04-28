package com.lody.virtual.server.apppermission;

import android.content.ClipData;
import android.content.IOnPrimaryClipChangedListener;
import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VAppPermissionManager;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.server.interfaces.IAppPermissionCallback;
import com.lody.virtual.server.interfaces.IAppPermissionManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by geyao on 2018/1/22.
 * 应用能力
 */

public class VAppPermissionManagerService extends IAppPermissionManager.Stub {
    private static final String TAG = VAppPermissionManagerService.class.getSimpleName();
    private static VAppPermissionManagerService sInstance;
    /**
     * 权限信息缓存map
     */
    private static Map<String, Boolean> functionMaps;
    /**
     * 权限监听
     */
    private IAppPermissionCallback iAppPermissionCallback;
    /**
     * 缓存剪切板信息
     */
    private static ClipData clipDataCache;
    /**
     * 缓存剪切板变化监听
     */
    private static IOnPrimaryClipChangedListener pcListener;

    public static void systemReady() {
        sInstance = new VAppPermissionManagerService();
        functionMaps = new HashMap<>();
    }

    public static VAppPermissionManagerService get() {
        return sInstance;
    }

    /**
     * 是否是支持的权限
     *
     * @param permissionName 权限名称
     * @return 是否是支持的权限
     */
    @Override
    public boolean isSupportPermission(String permissionName) {
        List<String> list = Arrays.asList(VAppPermissionManager.permissions);
        return list.contains(permissionName);
    }

    /**
     * 清除权限数据
     */
    @Override
    public void clearPermissionData() {
        functionMaps.clear();
    }

    /**
     * 设置应用权限开关
     *
     * @param packageName      应用包名
     * @param isPermissionOpen 开关状态
     */
    @Override
    public void setAppPermission(String packageName, String appPermissionName, boolean isPermissionOpen) {
        Log.e(TAG, "setAppPermission packageName: " + packageName);
        Log.e(TAG, "setAppPermission appPermissionName: " + appPermissionName);
        Log.e(TAG, "setAppPermission isPermissionOpen: " + isPermissionOpen);
        //若策略是关闭网络 则关闭应用进程的socket长链接
        if (VAppPermissionManager.PROHIBIT_NETWORK.equals(appPermissionName) && isPermissionOpen) {
            Log.e(TAG, "close long socket packageName: " + packageName);
            VActivityManager.get().closeAllLongSocket(packageName, 0);
        }
        functionMaps.put(buildKey(packageName, appPermissionName), isPermissionOpen);
        Log.e(TAG, "setAppPermission functionMaps: " + functionMaps.toString());
    }

    /**
     * 获取应用权限开关状态
     *
     * @param packageName 应用包名
     * @return 应用权限开关状态
     */
    @Override
    public boolean getAppPermissionEnable(String packageName, String appPermissionName) throws RemoteException {
        Log.e(TAG, "getAppPermissionEnable packageName: " + packageName);
        Log.e(TAG, "getAppPermissionEnable appPermissionName: " + appPermissionName);
        Log.e(TAG, functionMaps == null ? "getAppPermissionEnable map is null" : "getAppPermissionEnable map: " + functionMaps.toString());
        Boolean aBoolean = functionMaps.get(buildKey(packageName, appPermissionName));
        if (aBoolean == null) {
            Log.e(TAG, "result is null return false");
            return false;
        }
        Log.e(TAG, "result: " + aBoolean);
        return aBoolean;
    }

    /**
     * 注册监听
     *
     * @param iAppPermissionCallback 监听
     */
    @Override
    public void registerCallback(IAppPermissionCallback iAppPermissionCallback) throws RemoteException {
        VLog.e(TAG, "registerCallback");
        if (iAppPermissionCallback == null) {
            this.iAppPermissionCallback = null;
            return;
        }
        this.iAppPermissionCallback = iAppPermissionCallback;
    }

    /**
     * 解除监听注册
     */
    @Override
    public void unregisterCallback() throws RemoteException {
        VLog.e(TAG, "unregisterCallback");
        iAppPermissionCallback = null;
    }

    /**
     * 权限拦截触发回调
     *
     * @param appPackageName 应用包名
     * @param permissionName 权限名称
     */
    @Override
    public void interceptorTriggerCallback(String appPackageName, String permissionName) throws RemoteException {
        Log.d(TAG, "interceptorTriggerCallback appPackageName: " + appPackageName + " permissionName: " + permissionName);
        if (iAppPermissionCallback == null) {
            Log.d(TAG, "callback is null");
            return;
        }
        iAppPermissionCallback.onPermissionTrigger(appPackageName, permissionName);
    }

    /**
     * 缓存剪切板信息
     *
     * @param clipData 剪切板信息
     */
    @Override
    public void cacheClipData(ClipData clipData) throws RemoteException {
        Log.d(TAG, "cacheClipData clipData: " + (clipData == null ? "is null" : clipData.toString()));
        clipDataCache = clipData;
    }

    /**
     * 获取剪切板信息
     *
     * @return 剪切板信息
     */
    @Override
    public ClipData getClipData() throws RemoteException {
        Log.d(TAG, "getClipData clipDataCache: " + (clipDataCache == null ? "is null" : clipDataCache.toString()));
        return clipDataCache;
    }

    /**
     * 缓存剪切板数据改变监听
     *
     * @param listener 监听
     */
    @Override
    public void cachePrimaryClipChangedListener(IOnPrimaryClipChangedListener listener) throws RemoteException {
        Log.d(TAG, "cachePrimaryClipChangedListener");
        pcListener = listener;
    }

    /**
     * 获取剪切板数据改变监听
     */
    @Override
    public void callPrimaryClipChangedListener() throws RemoteException {
        Log.d(TAG, "callPrimaryClipChangedListener pcListener: " + (pcListener == null ? " listener is null " : " listener is not null"));
        if (pcListener == null) {
            return;
        }
        pcListener.dispatchPrimaryClipChanged();
    }

    /**
     * 移除剪切板数据改变监听
     */
    @Override
    public void removePrimaryClipChangedListener() throws RemoteException {
        Log.d(TAG, "removePrimaryClipChangedListener");
        pcListener = null;
    }

    /**
     * 构建缓存权限map的key
     *
     * @param packageName       应用包名
     * @param appPermissionName 权限名称
     * @return 生成的key
     */
    private String buildKey(String packageName, String appPermissionName) {
        return packageName + "," + appPermissionName;
    }
}