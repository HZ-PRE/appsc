let that = this;
var app_version = "";
var AppAccessKey ="";
var AdminAppAccessKey =false;
var ISUUSERAPP = false;
var appMsgList = [];
var appMsgAsyncs = {};
let dataa = {
	types: 'version',
	success: function(e) {
		let locaVersion = Number(e);
		app_version = `当前版本${locaVersion},最新版本${appVersion}`;
		if (e && appVersion && appVersion > locaVersion) {
			autolog.confirm("请更新", appVersionTxt, function() {
				autolog.warn("正在更新中，请等待...", -1)
				let d = 'https://pubtofilegz.oss-rg-china-mainland.aliyuncs.com/appsc/updateAll.zip';
				if (appVersion == (locaVersion + 1)) {
					d = 'https://pubtofilegz.oss-rg-china-mainland.aliyuncs.com/appsc/update.zip';
				}
				getUpdateApp("appUpdate", "APP更新", d+`?v=`+(new Date().getTime()))
			}, true,function(c){
				getAppOwner();
				c.close();
			});
		} else {
			getAppOwner();
		}
	}
}
getAppVersion(dataa);

function getAppOwner() {
	let data = {
		types: `appAccessKey`,
		success: function(e) {
			if (e && e['appAccessKey'] && APPACCESSKETLIST.includes(e['appAccessKey'])) {
				AppAccessKey=e['appAccessKey'];
				AdminAppAccessKey=ADMINAPPACCESSKETLIST.includes(e['appAccessKey'])
				ISUUSERAPP = true;
				_initApp_();
				if ('#0_home' === location.hash){
					autolog.success(`欢迎${e['appAccessKey']}来此happy！`)
				}
			} else {
				autolog.confirm(
					"密钥验证",
					`<input required type="text" id="appAccessKey" value="" class="input-box" placeholder="请选择输入你的密钥">`,
					success = function(e) {
						let appAccessKey = document.getElementById("appAccessKey").value;
						if (appAccessKey) {
							let saveD = {
								data: {
									type: 'appAccessKey',
									val: appAccessKey
								},
								success: function() {
									AppAccessKey=appAccessKey;
									AdminAppAccessKey=ADMINAPPACCESSKETLIST.includes(e['appAccessKey']);
									ISUUSERAPP = true;
									_initApp_();
									autolog.success("验证成功");
									e.close();
								},
								fail: function(r) {
									autolog.error(r);
								}
							}
							saveConfig(saveD)
						}
					})
			};
		}
	}
	getAppConfig(data);
}
/**
 * 全局消息
 * soupage 当前的页面hash
 * text 需要放置的消息
 * target  当前页面需要显示的区域
 * isAsync 是否在离开当前页，开启全局消息
 * async  异步消息的唯一变量
 * @param {string|HTMLElement} target - 元素ID 或 直接传 HTMLElement
 * @param {string} text - 要设置的文本内容
 */
function safeSetText(soupage, text = "", target, isAsync = false, async = "") {
	function setContent() {
		let el = typeof target === "string" ? document.getElementById(target) : target;
		if (el) el.innerText = text;
	}
	let newLocaHash = location.hash.substring(1);
	// DOM 已经加载完成
	if (soupage === newLocaHash && target !== "") {
		setContent();
	} else if ("" !== text) {
		if (isAsync && "" !== async && !appMsgAsyncs.hasOwnProperty(`${async}`)) {
			appMsgAsyncs[async] = (appMsgList.length > 0?appMsgList.length - 1 : 0);
		};
		if (isAsync && "" !== async && appMsgAsyncs.hasOwnProperty(`${async}`)) {
			appMsgList[appMsgAsyncs[async]] = text;
		} else {
			appMsgList.push(text);
		}
		getAppMsgList();
	}
}

function delAppMsgList(t, e) {
	if (0 === t) {
		appMsgList = []
	} else {
		appMsgList.splice(e, 1);
	}
	getAppMsgList();
}

function getAppMsgList() {
	let msg = '<span onclick="delAppMsgList(0)">清空全部</span>';
	for (var i = 0; i < appMsgList.length; i++) {
		msg += `<br/><span><a onclick="delAppMsgList(1,${i})">❌</a> ${appMsgList[i]}</span>`;
	}
	document.getElementById("app-page-return").innerHTML = appMsgList.length>0?msg:'';
}

function getPageHash(e) {
	return location.hash.substring(1) === e;
}

/**
 * 下载文件
 * api  api
 * text 下载文件的主题
 * data  需要传递的参数
 * target  返回结果时，当前页面需要显示的区域
 */
function getUpdateApp(api, text = "", data = null, target = "") {
	const newLocaHash = location.hash.substring(1);
	const appMsgAsyncsName = new Date().getTime();
	let call = {
		data: data,
		api: api,
		type: 1,
		success: function(e) {
			var progressTime = setInterval(function() {
				let data1 = {
					data: data,
					api: api,
					type: 0,
					success: function(r) {
						if (r.startsWith("100")) {
							safeSetText(newLocaHash, `✅ ${text}-成功`, target, true,
								appMsgAsyncsName);
							clearInterval(progressTime);
						} else {
							safeSetText(newLocaHash, `${text}-进度: ${r}%`, target, true,
								appMsgAsyncsName);
						}
					},
					fail: function(e1) {
						safeSetText(newLocaHash, `${text}:${e1}`, target, true,
							appMsgAsyncsName);
						clearInterval(progressTime);
					}
				}
				getDownloadFile(data1);
			}, 1000)
		},
		fail: function(e) {
			autolog.error(e);
		}
	}
	getDownloadFile(call);
}
//将字符串首写字母转小写
function lowercaseFirstLetter(str) {
	if (!str) return str;
	return str.charAt(0).toLowerCase() + str.slice(1);
}
//检测是否含有i位的数字
function checkForLongNumbers(str,i=4) {
	const pattern = new RegExp(`\\d{${i},}`, 'g'); // 匹配至少5位的数字串
	return str.match(pattern);
}

//应用类型
const APPTYPES=[
	{
		"left":"首页",
		"top":[
			{
				"tit":"首页",
				"a":"home"
			}
		]
	},
	{
		"left":"应用",
		"top":[
			{
				"tit":"上传安装包",
				"a":"instartll"
			},{
				"tit":"安装包回滚",
				"a":"rollback"
			},{
				"tit":"云上传",
				"a":"yun"
			},{
				"tit":"源地址",
				"a":"host_pwd"
			},{
				"tit":"客服修改",
				"a":"kf_url"
			}
		]
	},
	{
		"left":"工具",
		"top":[
			{
				"tit":"去查ip",
				"a":"selip"
			},{
				"tit":"发邮件",
				"a":"mail"
			},{
				"tit":"加密解密",
				"a":"password"
			}
		]
	},
	{
		"left":"服务",
		"top":[
			{
				"tit":"链接监控",
				"a":"server_url"
			},
			{
				"tit":"域名管理",
				"a":"server_host"
			},
			{
				"tit":"授权API",
				"a":"server_yunapi"
			},
			{
				"tit":"节点管理",
				"a":"server_node"
			}
		]
	}
]