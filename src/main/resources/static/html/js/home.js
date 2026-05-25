window.home = {
    data: {},

    created() {
        const newDate = new Date();
        const isSpecialDay = newDate.getMonth() === 4 && newDate.getDate() === 21;

        if (isSpecialDay) {
            document.querySelector(".inner")?.style.setProperty("display", "none");
            const versionEl = document.getElementById("version");
            if (versionEl) versionEl.hidden = true;
            const jlrEl = document.getElementById("jlr");
            if (jlrEl) jlrEl.hidden = false;
            return;
        }

        this.initPandaAnimation();
        this.initThemeToggle();
        this.initSnackButton();
        this.initPandaClick();

        const versionEl = document.getElementById("version");
        if (versionEl) versionEl.innerText = app_version;
    },

    initPandaAnimation() {
        const btnWave = document.getElementById('btnWave');
        const pandaWrap = document.getElementById('pandaWrap');
        if (!btnWave || !pandaWrap) return;

        let waving = false;
        btnWave.addEventListener('click', () => {
            if (waving) return;
            waving = true;

            const arm = pandaWrap.querySelector('.arm-right');
            arm?.animate([
                { transform: 'rotate(0deg)' },
                { transform: 'rotate(-50deg) translateY(-10px)' },
                { transform: 'rotate(0deg)' }
            ], { duration: 1000, easing: 'ease-in-out' });

            setTimeout(() => waving = false, 1100);
        });
    },

    initThemeToggle() {
        const btnTheme = document.getElementById('btnTheme');
        if (!btnTheme) return;

        const root = document.documentElement;
        let pink = false;

        btnTheme.addEventListener('click', () => {
            pink = !pink;
            if (pink) {
                root.style.setProperty('--bamboo', '#e57fa8');
                root.style.setProperty('--accent', '#f9a6c0');
                root.style.setProperty('--bg', 'linear-gradient(180deg,#fff0f8 0%, #fffafb 60%)');
            } else {
                root.style.setProperty('--bamboo', '#76b041');
                root.style.setProperty('--accent', '#7ecf6e');
                root.style.setProperty('--bg', 'linear-gradient(180deg,#cfe9ff 0%, #f7fbff 60%)');
            }
        });
    },

    initSnackButton() {
        const btnSnack = document.getElementById('btnSnack');
        const pandaWrap = document.getElementById('pandaWrap');
        if (!btnSnack || !pandaWrap) return;

        btnSnack.addEventListener('click', () => {
            const snack = pandaWrap.querySelector('#bambooSnack');
            snack?.animate([
                { transform: 'translate(140px,190px) scale(0.9)' },
                { transform: 'translate(140px,190px) scale(1.08)' },
                { transform: 'translate(140px,190px) scale(0.95)' },
                { transform: 'translate(140px,190px) scale(1)' }
            ], { duration: 500, easing: 'cubic-bezier(.2,.8,.2,1)' });
        });
    },

    initPandaClick() {
        const pandaWrap = document.getElementById('pandaWrap');
        if (!pandaWrap) return;

        pandaWrap.addEventListener('click', () => {
            pandaWrap.animate([
                { transform: 'scale(1)' },
                { transform: 'scale(1.04)' },
                { transform: 'scale(1)' }
            ], { duration: 420, easing: 'cubic-bezier(.2,.8,.2,1)' });
        });
    },

    methods: {
        setAppConf() {
            autolog.confirm(
                "请选择配置",
                `<input type="file" id="fileAppConf" accept=".conf" />`,
                (e) => {
                    const fileInput = document.getElementById("fileAppConf");
                    const file = fileInput?.files?.[0];
                    if (!file) {
                        autolog.warn("请选择文件");
                        return;
                    }

                    const fileName = file.name;
                    if (fileName.includes("（") || fileName.includes("）") || !fileName.endsWith(".conf")) {
                        autolog.warn("文件名不能含有全角括号，且必须为.conf文件");
                        return;
                    }

                    const data = new FormData();
                    data.append("appfile", file);

                    saveFileAppConf({
                        data,
                        success: () => {
                            autolog.success("配置成功");
                            e.close();
                            setTimeout(() => location.reload(), 1000);
                        },
                        fail: (r) => autolog.error(r)
                    });
                },
                true
            );
        }
    }
};
