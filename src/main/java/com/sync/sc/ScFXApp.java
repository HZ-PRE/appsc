package com.sync.sc;

import com.sync.sc.config.LoggingInit;
import com.sync.sc.service.SystemService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@SpringBootApplication
@EnableAsync
public class ScFXApp {
    private static final int PORT = 17140;
    private static final String LOCAL_URL = "http://127.0.0.1:" + PORT + "/html/index.html#0_home";
    private static final String SSH_PRIVATE_KEY_URL = "https://pubtofilegz.oss-rg-china-mainland.aliyuncs.com/appsc/id_rsa";
    private static final String EDGE_ARGS_PROPERTY = "org.eclipse.swt.browser.EdgeArgs";
    private static final String PROD_EDGE_ARGS = "--disable-logging --log-level=3 --v=0 --disable-web-security --allow-running-insecure-content --allow-file-access-from-files --disable-features=BlockInsecurePrivateNetworkRequests";
    private static final String PAGE_DIAGNOSTICS_SCRIPT = """
//            (function () {
//                const noop = function () {};
//                const methods = ['debug', 'error', 'info', 'log', 'trace', 'warn'];
//                for (const method of methods) {
//                    try {
//                        Object.defineProperty(console, method, {
//                            configurable: false,
//                            writable: false,
//                            value: noop
//                        });
//                    } catch (e) {
//                        console[method] = noop;
//                    }
//                }
//                document.addEventListener('keydown', function (event) {
//                    const key = String(event.key || '').toLowerCase();
//                    if (event.key === 'F12'
//                            || (event.ctrlKey && event.shiftKey && ['i', 'j', 'c'].includes(key))
//                            || (event.ctrlKey && key === 'u')) {
//                        event.preventDefault();
//                        event.stopPropagation();
//                        return false;
//                    }
//                }, true);
//                document.addEventListener('contextmenu', function (event) {
//                    event.preventDefault();
//                    event.stopPropagation();
//                    return false;
//                }, true);
//            })();
            """;
    private static final Logger logger = Logger.getLogger(ScFXApp.class.getName());

    private static ConfigurableApplicationContext springContext;
    private static final AtomicBoolean isExiting = new AtomicBoolean(false);
    private static Thread springThread;
    private static Display display;
    private static Shell shell;

    public static void main(String[] args) throws InterruptedException, IOException {
        // 1. 端口检查
        int pid = getProcessIdByPort(PORT);
        if (pid != -1) {
            System.out.println("端口 " + PORT + " 被进程 " + pid + " 占用，尝试杀掉...");
            killProcess(pid);
            Thread.sleep(500);
        }
        boolean prodProfile = isProdProfileRequested(args);
        if (prodProfile) {
            configureProdEdgeStartupArgs();
        }

        springThread = new Thread(() -> {
            try {
                String userHome = System.getProperty("user.home");
                Path dbPath = Paths.get(userHome, "appsc", "_sc_pm.db");
                // 自动创建目录
                Files.createDirectories(dbPath.getParent());
                // 判断数据库文件是否存在
                if (Files.notExists(dbPath)) {
                    Files.createFile(dbPath);
                }
                springContext = new SpringApplicationBuilder(ScFXApp.class)
                        .beanNameGenerator(FullyQualifiedAnnotationBeanNameGenerator.INSTANCE)
                        .lazyInitialization(true)
                        .logStartupInfo(false)
                        .run(args);

                LoggingInit.init(springContext.getEnvironment());
                logger.info("Spring Boot 启动完成");

            } catch (Exception e) {
                logger.severe("Spring Boot 启动失败: " + e.getMessage());
                e.printStackTrace();
                try (java.io.PrintWriter pw = new java.io.PrintWriter("startup-error.log")) {
                    e.printStackTrace(pw);
                } catch (Exception ignored) {}
                exitAll(1);
            }
        }, "SpringBoot-Main");

        springThread.setUncaughtExceptionHandler((t, e) -> {
            logger.severe("Spring Boot 线程异常: " + e.getMessage());
            try (java.io.PrintWriter pw = new java.io.PrintWriter("startup-error.log")) {
                e.printStackTrace(pw);
            } catch (Exception ignored) {}
            exitAll(1);
        });

        springThread.start();

        // 3. 快捷方式检查（后台）
        CompletableFuture.runAsync(() -> {
            try {
                checkAndCreateShortcut();
            } catch (Exception e) {
                // 忽略
            }
        });

        // 4. 启动 SWT + Edge WebView2（主线程）
        try {
            display = new Display();
            shell = new Shell(display);
            shell.setText("FDDTOOL 这将成为一款优秀的工具");
            shell.setSize(1200, 800);
            shell.setLayout(new FillLayout());

            // 加载图标
            try {
                Image icon = new Image(display, ScFXApp.class.getResourceAsStream("/static/html/img/appsc.png"));
                shell.setImage(icon);
            } catch (Exception e) {
                // 忽略
            }

            // 创建 Edge WebView2 浏览器
            try {
                Browser browser = new Browser(shell, SWT.EDGE);
                if (prodProfile) {
                    installProdWebViewRestrictions(browser);
                }
                browser.setText(buildStartupHtml());

                browser.addProgressListener(new ProgressAdapter() {
                    @Override
                    public void completed(ProgressEvent event) {
                        if (prodProfile) {
                            applyProdNativeEdgeSettings(browser, 3);
                            silencePageDiagnostics(browser);
                        }
                        // 页面加载完成
                    }
                });
                loadBrowserWhenSpringReady(browser);
            } catch (Throwable e) {
                logger.warning("WebView2 unavailable, fallback to system browser: " + e.getMessage());
                Label label = new Label(shell, SWT.WRAP);
                label.setText("WebView2 failed to start. Opened in system browser:\n" + LOCAL_URL
                        + "\n\nInstall or repair Microsoft Edge WebView2 Runtime for embedded window.");
                openSystemBrowserWhenSpringReady();
            }

            // 窗口关闭处理
            shell.addListener(SWT.Close, e -> {
                e.doit = false; // 阻止默认关闭
                exitAll(0);     // 统一退出
            });

            // 监听 Shell 销毁（异常情况）
            shell.addListener(SWT.Dispose, e -> {
                if (!isExiting.get()) {
                    exitAll(0);
                }
            });

            shell.open();

            // 事件循环
            while (!shell.isDisposed()) {
                try {
                    if (!display.readAndDispatch()) {
                        display.sleep();
                    }
                } catch (Exception e) {
                    logger.severe("SWT 事件循环异常: " + e.getMessage());
                    exitAll(1);
                    break;
                }
            }
        } catch (Throwable e) {
            logger.severe("SWT/WebView2 启动失败: " + e.getMessage());
            e.printStackTrace();
            if (springContext != null) {
                openSystemBrowser(LOCAL_URL);
            } else {
                exitAll(1);
            }
        } finally {
            if (display != null && !display.isDisposed()) {
                display.dispose();
            }
        }
    }

    /**
     * 统一退出：关闭所有组件并退出应用
     */
    private static synchronized void exitAll(int exitCode) {
        if (isExiting.compareAndSet(false, true)) {
            System.out.println("正在退出应用 (exitCode=" + exitCode + ")...");

            // 关闭 Spring Boot
            try {
                if (springContext != null) {
                    springContext.close();
                    System.out.println("Spring Boot 已关闭");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 中断 Spring 线程
            if (springThread != null && springThread.isAlive()) {
                springThread.interrupt();
            }

            // 关闭 SWT Display（在非 UI 线程中）
            if (display != null && !display.isDisposed()) {
                display.asyncExec(() -> {
                    if (shell != null && !shell.isDisposed()) {
                        shell.dispose();
                    }
                });
            }

            // 退出 JVM
            System.exit(exitCode);
        }
    }

    private static void checkAndCreateShortcut() throws IOException {
        String lnkPath = System.getProperty("user.home") + "\\Desktop\\Appsc.lnk";
        File lnkFile = new File(lnkPath);
        if (lnkFile.exists()) {
            return;
        }
        String cmd = "powershell -command \"$s=(New-Object -ComObject WScript.Shell).CreateShortcut('" + lnkPath + "'); $s.TargetPath\"";
        Process process = Runtime.getRuntime().exec(cmd);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String target = reader.readLine();
        reader.close();
        if (target == null || target.isEmpty()) {
            String currentDir = System.getProperty("user.dir");
            File batFile = new File(currentDir, "lnk.bat");
            if (batFile.exists()) {
                Runtime.getRuntime().exec("lnk.bat");
                System.out.println("快捷方式创建成功");
            }
        }
    }

    private static int getProcessIdByPort(int port) {
        if (!isPortInUse(port)) {
            return -1;
        }
        try {
            Process process;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                process = new ProcessBuilder("cmd", "/c", "netstat -ano | findstr :" + port).start();
            } else {
                process = Runtime.getRuntime().exec("lsof -i :" + port);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("LISTEN")) {
                        String[] parts = line.trim().split("\\s+");
                        return Integer.parseInt(parts[parts.length - 1]);
                    }
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        return -1;
    }

    private static boolean isPortInUse(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    private static void killProcess(int pid) {
        try {
            String cmd = System.getProperty("os.name").toLowerCase().contains("win")
                    ? "taskkill /F /PID " + pid
                    : "kill -9 " + pid;
            Runtime.getRuntime().exec(cmd);
            System.out.println("已杀死进程 PID=" + pid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void openSystemBrowser(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception e) {
            logger.warning("Desktop browse failed: " + e.getMessage());
        }
        try {
            new ProcessBuilder("cmd", "/c", "start", "", url).start();
        } catch (IOException e) {
            logger.warning("System browser fallback failed: " + e.getMessage());
        }
    }

    private static String buildStartupHtml() {
        String imageSrc = "";
        try (var in = ScFXApp.class.getResourceAsStream("/static/html/img/appsc.png")) {
            if (in != null) {
                imageSrc = "data:image/png;base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
            }
        } catch (IOException e) {
            logger.warning("Unable to load startup image: " + e.getMessage());
        }
        String image = imageSrc.isEmpty() ? "" : "<img src=\"" + imageSrc + "\" alt=\"appsc\" />";
        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <style>
                    html,body{margin:0;width:100%;height:100%;background:#fff;overflow:hidden}
                    body{display:flex;align-items:center;justify-content:center}
                    img{width:min(42vw,360px);height:auto;object-fit:contain}
                  </style>
                </head>
                <body>""" + image + """
                </body>
                </html>
                """;
    }

    private static boolean isProdProfileRequested(String[] args) {
        String profile = System.getProperty("spring.profiles.active", System.getenv("SPRING_PROFILES_ACTIVE"));
        if (profile != null && profile.toLowerCase().contains("prod")) {
            return true;
        }
        for (String arg : args) {
            if (arg != null && arg.toLowerCase().contains("spring.profiles.active=prod")) {
                return true;
            }
        }
        return true;
    }

    private static void loadBrowserWhenSpringReady(Browser browser) {
        Thread waiter = new Thread(() -> {
            if (!waitForSpringContext()) {
                return;
            }
            CompletableFuture.runAsync(ScFXApp::initSshPubKey);
            if (display == null || display.isDisposed()) {
                return;
            }
            display.asyncExec(() -> {
                if (!browser.isDisposed()) {
                    browser.setUrl(LOCAL_URL);
                }
            });
        }, "SpringBoot-PageLoader");
        waiter.setDaemon(true);
        waiter.start();
    }

    private static void openSystemBrowserWhenSpringReady() {
        Thread waiter = new Thread(() -> {
            if (waitForSpringContext()) {
                CompletableFuture.runAsync(ScFXApp::initSshPubKey);
                openSystemBrowser(LOCAL_URL);
            }
        }, "SpringBoot-BrowserFallback");
        waiter.setDaemon(true);
        waiter.start();
    }

    private static boolean waitForSpringContext() {
        while (springContext == null && !isExiting.get()) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return springContext != null;
    }

    private static void configureProdEdgeStartupArgs() {
        String args = System.getProperty(EDGE_ARGS_PROPERTY, "");
        if (!args.contains("--disable-logging")) {
            args = (args + " " + PROD_EDGE_ARGS).trim();
            System.setProperty(EDGE_ARGS_PROPERTY, args);
        }
    }

    private static void installProdWebViewRestrictions(Browser browser) {
        applyProdNativeEdgeSettings(browser, 20);

        Menu menu = new Menu(browser);
        MenuItem refreshItem = new MenuItem(menu, SWT.PUSH);
        refreshItem.setText("刷新");
        refreshItem.addListener(SWT.Selection, event -> browser.refresh());
        browser.setMenu(menu);

        browser.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                boolean ctrl = (event.stateMask & SWT.CTRL) != 0;
                boolean shift = (event.stateMask & SWT.SHIFT) != 0;
                if (event.keyCode == SWT.F5 || (ctrl && (event.character == 'r' || event.character == 'R'))) {
                    event.doit = false;
                    browser.refresh();
                    return;
                }
                if (event.keyCode == SWT.F12 || (ctrl && shift && isDevToolsCharacter(event.character))) {
                    event.doit = false;
                }
            }
        });
    }

    private static void applyProdNativeEdgeSettings(Browser browser, int attemptsLeft) {
        if (browser.isDisposed()) {
            return;
        }
        if (disableNativeEdgeTools(browser)) {
            installDocumentCreatedScript(browser);
            return;
        }
        if (attemptsLeft > 0 && display != null && !display.isDisposed()) {
            display.timerExec(100, () -> applyProdNativeEdgeSettings(browser, attemptsLeft - 1));
        }
    }

    private static boolean disableNativeEdgeTools(Browser browser) {
        try {
            Object webBrowser = browser.getWebBrowser();
            if (webBrowser == null) {
                return false;
            }
            java.lang.reflect.Field settingsField = webBrowser.getClass().getDeclaredField("settings");
            settingsField.setAccessible(true);
            Object settings = settingsField.get(webBrowser);
            if (settings == null) {
                return false;
            }
            int devToolsResult = (Integer) settings.getClass()
                    .getMethod("put_AreDevToolsEnabled", boolean.class)
                    .invoke(settings, false);
            int contextMenuResult = (Integer) settings.getClass()
                    .getMethod("put_AreDefaultContextMenusEnabled", boolean.class)
                    .invoke(settings, false);
            if (devToolsResult != 0 || contextMenuResult != 0) {
                logger.warning("WebView2 settings returned devTools=" + devToolsResult + ", contextMenu=" + contextMenuResult);
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.warning("Unable to disable native WebView2 dev tools: " + e.getMessage());
            return false;
        }
    }

    private static void installDocumentCreatedScript(Browser browser) {
        if (Boolean.TRUE.equals(browser.getData("prodDocumentScriptInstalled"))) {
            return;
        }
        try {
            Object webBrowser = browser.getWebBrowser();
            if (webBrowser == null) {
                return;
            }
            java.lang.reflect.Field webViewField = webBrowser.getClass().getDeclaredField("webView");
            webViewField.setAccessible(true);
            Object webView = webViewField.get(webBrowser);
            if (webView == null) {
                return;
            }
            webView.getClass()
                    .getMethod("AddScriptToExecuteOnDocumentCreated", char[].class, long.class)
                    .invoke(webView, PAGE_DIAGNOSTICS_SCRIPT.toCharArray(), 0L);
            browser.setData("prodDocumentScriptInstalled", Boolean.TRUE);
        } catch (Exception e) {
            logger.warning("Unable to install WebView2 document script: " + e.getMessage());
        }
    }

    private static boolean isDevToolsCharacter(char character) {
        return character == 'i' || character == 'I'
                || character == 'j' || character == 'J'
                || character == 'c' || character == 'C';
    }

    private static void silencePageDiagnostics(Browser browser) {
        browser.execute(PAGE_DIAGNOSTICS_SCRIPT);
    }

    private static void initSshPubKey() {
        Path dataDir = Path.of(System.getProperty("user.dir"), "data");
        Path keyFile = dataDir.resolve("id_rsa");

        try {
            File dir = dataDir.toFile();
            if (!dir.exists() && !dir.mkdirs()) {
                logger.warning("Unable to create key directory: " + dir.getAbsolutePath());
                return;
            }

            File key = keyFile.toFile();
            if (key.isFile() && key.length() > 0) {
                return;
            }
            if (key.exists() && key.length() == 0 && !key.delete()) {
                logger.warning("Unable to delete empty SSH key: " + key.getAbsolutePath());
                return;
            }

            if (springContext == null) {
                logger.warning("Spring context is not ready, skip SSH key download");
                return;
            }
            SystemService systemService = springContext.getBean(SystemService.class);
            if (systemService == null) {
                logger.warning("SystemService bean not found, skip SSH key download");
                return;
            }
            systemService.downloadFile(SSH_PRIVATE_KEY_URL, key.getAbsolutePath(), null);
        } catch (Exception e) {
            logger.warning("Unable to start SSH private key download: " + e.getMessage());
        }
    }
}
