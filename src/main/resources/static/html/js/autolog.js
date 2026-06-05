const cssLogStr = `#autolog24516{display:flex;flex-direction:column;align-items:center;justify-content:flex-start;pointer-events:none;width:100vw;height:100vh;position:fixed;left:0;top:0;z-index:9999999;cursor:pointer;transition:0.2s}#autolog24516 span{pointer-events:auto;width:max-content;animation:fadein 0.4s;animation-delay:0s;border-radius:6px;padding:10px 20px;box-shadow:0 0 10px 6px rgba(0,0,0,0.1);margin:4px;transition:0.2s;z-index:9999999;font-size:14px;display:flex;align-items:center;justify-content:center;gap:4px;height:max-content}#autolog24516 span.hide{opacity:0;pointer-events:none;transform:translateY(-10px);height:0;padding:0;margin:0}.autolog24516-warn{background-color:#fffaec;color:#e29505}.autolog24516-error{background-color:#fde7e7;color:#d93025}.autolog24516-info{background-color:#e6f7ff;color:#0e6eb8}.autolog24516-success{background-color:#e9f7e7;color:#1a9e2c}.autolog24516-{background-color:#fafafa;color:#333}@keyframes fadein{0%{opacity:0;transform:translateY(-10px)}100%{opacity:1;transform:translateY(0)}}`;
const svgLogIcons = {
    warn: `<svg t="1713405237257" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="2387" xmlns:xlink="http://www.w3.org/1999/xlink" width="16" height="16"><path d="M934.4 770.133333L605.866667 181.333333C586.666667 147.2 550.4 128 512 128c-38.4 0-74.666667 21.333333-93.866667 53.333333L89.6 770.133333c-19.2 34.133333-19.2 76.8 0 110.933334S145.066667 938.666667 183.466667 938.666667h657.066666c38.4 0 74.666667-21.333333 93.866667-57.6 19.2-34.133333 19.2-76.8 0-110.933334z m-55.466667 81.066667c-8.533333 14.933333-23.466667 23.466667-38.4 23.466667H183.466667c-14.933333 0-29.866667-8.533333-38.4-23.466667-8.533333-14.933333-8.533333-34.133333 0-49.066667L473.6 213.333333c8.533333-12.8 23.466667-21.333333 38.4-21.333333s29.866667 8.533333 38.4 21.333333l328.533333 588.8c8.533333 14.933333 8.533333 32 0 49.066667z" fill="#e29505" p-id="2388"></path><path d="M512 746.666667m-42.666667 0a42.666667 42.666667 0 1 0 85.333334 0 42.666667 42.666667 0 1 0-85.333334 0Z" fill="#e29505" p-id="2389"></path><path d="M512 629.333333c17.066667 0 32-14.933333 32-32v-192c0-17.066667-14.933333-32-32-32s-32 14.933333-32 32v192c0 17.066667 14.933333 32 32 32z" fill="#e29505" p-id="2390"></path></svg>`,
    error: `<svg t="1713405212725" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="1744" xmlns:xlink="http://www.w3.org/1999/xlink" width="16" height="16"><path d="M512 74.666667C270.933333 74.666667 74.666667 270.933333 74.666667 512S270.933333 949.333333 512 949.333333 949.333333 753.066667 949.333333 512 753.066667 74.666667 512 74.666667z m0 810.666666c-204.8 0-373.333333-168.533333-373.333333-373.333333S307.2 138.666667 512 138.666667 885.333333 307.2 885.333333 512 716.8 885.333333 512 885.333333z" fill="#d93025" p-id="1745"></path><path d="M657.066667 360.533333c-12.8-12.8-32-12.8-44.8 0l-102.4 102.4-102.4-102.4c-12.8-12.8-32-12.8-44.8 0-12.8 12.8-12.8 32 0 44.8l102.4 102.4-102.4 102.4c-12.8 12.8-12.8 32 0 44.8 6.4 6.4 14.933333 8.533333 23.466666 8.533334s17.066667-2.133333 23.466667-8.533334l102.4-102.4 102.4 102.4c6.4 6.4 14.933333 8.533333 23.466667 8.533334s17.066667-2.133333 23.466666-8.533334c12.8-12.8 12.8-32 0-44.8l-106.666666-100.266666 102.4-102.4c12.8-12.8 12.8-34.133333 0-46.933334z" fill="#d93025" p-id="1746"></path></svg>`,
    info: `<svg t="1713405208589" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="1582" xmlns:xlink="http://www.w3.org/1999/xlink" width="16" height="16"><path d="M853.333333 138.666667H170.666667c-40.533333 0-74.666667 34.133333-74.666667 74.666666v512c0 40.533333 34.133333 74.666667 74.666667 74.666667h151.466666V917.333333c0 12.8 8.533333 25.6 19.2 29.866667 4.266667 2.133333 8.533333 2.133333 12.8 2.133333 8.533333 0 17.066667-4.266667 23.466667-10.666666l136.533333-138.666667H853.333333c40.533333 0 74.666667-34.133333 74.666667-74.666667V213.333333c0-40.533333-34.133333-74.666667-74.666667-74.666666z m10.666667 586.666666c0 6.4-4.266667 10.666667-10.666667 10.666667H501.333333c-8.533333 0-17.066667 4.266667-23.466666 10.666667l-89.6 93.866666V768c0-17.066667-14.933333-32-32-32H170.666667c-6.4 0-10.666667-4.266667-10.666667-10.666667V213.333333c0-6.4 4.266667-10.666667 10.666667-10.666666h682.666666c6.4 0 10.666667 4.266667 10.666667 10.666666v512z" fill="#0e6eb8" p-id="1583"></path><path d="M512 490.666667H298.666667c-17.066667 0-32 14.933333-32 32S281.6 554.666667 298.666667 554.666667h213.333333c17.066667 0 32-14.933333 32-32S529.066667 490.666667 512 490.666667zM672 341.333333H298.666667c-17.066667 0-32 14.933333-32 32S281.6 405.333333 298.666667 405.333333h373.333333c17.066667 0 32-14.933333 32-32s-14.933333-32-32-32z" fill="#0e6eb8" p-id="1584"></path></svg>`,
    success: `<svg t="1713405224326" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="2225" xmlns:xlink="http://www.w3.org/1999/xlink" width="16" height="16"><path d="M512 74.666667C270.933333 74.666667 74.666667 270.933333 74.666667 512S270.933333 949.333333 512 949.333333 949.333333 753.066667 949.333333 512 753.066667 74.666667 512 74.666667z m0 810.666666c-204.8 0-373.333333-168.533333-373.333333-373.333333S307.2 138.666667 512 138.666667 885.333333 307.2 885.333333 512 716.8 885.333333 512 885.333333z" fill="#1a9e2c" p-id="2226"></path><path d="M701.866667 381.866667L448 637.866667 322.133333 512c-12.8-12.8-32-12.8-44.8 0-12.8 12.8-12.8 32 0 44.8l149.333334 149.333333c6.4 6.4 14.933333 8.533333 23.466666 8.533334s17.066667-2.133333 23.466667-8.533334l277.333333-277.333333c12.8-12.8 12.8-32 0-44.8-14.933333-12.8-36.266667-12.8-49.066666-2.133333z" fill="#1a9e2c" p-id="2227"></path></svg>`,
};
const autolog = {
    error(text, time = 2500){
        return this.log(text,"error", time);
    },
    success(text, time = 2500){
        return this.log(text,"success", time);
    },
    warn(text, time = 2500){
        return this.log(text,"warn", time);
    },
    info(text, time = 2500){
        return this.log(text,"info", time);
    },
    log(text, type = "", time = 2500) {
        if (typeof type === "number") {
            time = type;
            type = "";
        }
        if(svgLogIcons[type] == undefined){
            type = "";
        }
        let mainEl = getMainElement24516();
        let el = document.createElement("span");
        if(type==""){
            el.innerHTML = text;
        }else {
            el.className = `autolog24516-${type}`;
            el.innerHTML = svgLogIcons[type] + text;
        }
        mainEl.appendChild(el);
		if(-1 != time){
			setTimeout(() => {
				el.classList.add("hide");
			}, time - 500);
			setTimeout(() => {
				mainEl.removeChild(el);
				el = null;
			}, time);
            return null
		}else{
            return function (){
                el.classList.add("hide");
                mainEl.removeChild(el);
                el = null;
            }
        }
    },
	confirm(title,text,success, isClose,fail,other = {}){
		handleShowConfirm24516(title,text,success, isClose,fail,other['successTit'],other['failTit'],other['maxWidth'],other['isCope'],other['copeTit'],other['copeFun']);
	}
};
function getMainElement24516() {
    let mainEl = document.querySelector("#autolog24516");
    if (!mainEl) {
        mainEl = document.createElement("div");
        mainEl.id = "autolog24516";
        document.body.appendChild(mainEl);
        let style = document.createElement("style");
        style.innerHTML = cssLogStr;
        document.head.insertBefore(style, document.head.firstChild);
    }
    return mainEl;
}

function handleShowConfirm24516(title,text,success, isClose = false,fail,successTit='确定',failTit='取消',maxWidth = 400,isCope = false,copeTit='复制',copeFun) {
	document.body.insertAdjacentHTML("beforeend", 
`<div id="confirmModal25219" class="modal" style="
		  position: fixed;
		  z-index: 9999;
		  left: 0;
		  top: 0;
		  width: 100%;
		  height: 100%;
		  overflow: auto;
		  background-color: rgb(0,0,0);
		  background-color: rgba(0,0,0,0.4);
		  ">
	  <div style="border-radius: 0.5rem;
		  background-color: #fefefe;
		  margin: 15% auto;padding: 0 0.8rem 0.8rem 0.8rem;
		  border: 1px solid #888;
		  width: 80%;word-wrap: break-word;
		  max-width: ${maxWidth}px;">
	    <h4>${title}</h4>
	    <div id="confirmModalText25219">${text}</div>
		<div style="display: flex;justify-content: flex-end;">
			<button id="handleButCopeFun2643" ${isCope?'':'hidden'} style="background: #fff;border-radius: 0.5rem;border: 0.0625rem solid #888;padding: 0.3rem 0.5rem;">${copeTit}</button>
			<button id="handleBut25219" ${isClose?'':'hidden'} style="background: #fff;border-radius: 0.5rem;border: 0.0625rem solid #888;padding: 0.3rem 0.5rem;margin-left:0.1rem;color: #000">${failTit}</button>
			<button id="handleBut25220" style="background: #6366f1;border-radius: 0.5rem;border: 0.0625rem solid #888;padding: 0.3rem 0.5rem;color: #fff;margin-left:0.1rem">${successTit}</button>
		</div>
	  </div>
	</div>`);
    let confirmModalText25219 =document.getElementById("confirmModalText25219");
	let callD={
		close:throttle(() => {
		  document.getElementById('confirmModal25219').remove();
		}, 5000)
	}
	document.getElementById('handleBut25219').addEventListener('click', throttle(() => {
		  if(fail){
		  	fail(callD,confirmModalText25219)
		  }else{
		  	callD.close();
		  }
		}, 5000));
	document.getElementById('handleBut25220').addEventListener('click', throttle(() => {
		  if(success){
		  	success(callD,confirmModalText25219)
		  }else{
		  	callD.close();
		  }
		}, 5000));
	document.getElementById('handleButCopeFun2643').addEventListener('click', throttle(() => {
		  if(copeFun){
			copeFun(confirmModalText25219)
		  }else{
		  	autolog.warn("没有找到复制对象")
		  }
		}, 5000));
}
//限流
function throttle(fn, delay) {
  let last = 0;
  return function (...args) {
    const now = Date.now();
    if (now - last >= delay) {
      fn.apply(this, args);
      last = now;
    }
  };
}
//复制
function copyToClipboard(text) {
    if (navigator.clipboard) {
        return navigator.clipboard.writeText(text);
    } else {
        // 兼容老浏览器
        const textarea = document.createElement("textarea");
        textarea.value = text;
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand("copy");
        document.body.removeChild(textarea);
        return Promise.resolve();
    }
}
//复制用法
function copyText(text,t='复制成功'){
    copyToClipboard(text).then(() => {
        autolog.success(t);
    });
}
function shuffleArray(array) {
    for (let i = array.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [array[i], array[j]] = [array[j], array[i]]; // ES6 解构赋值
    }
    return array;
}
function getDomainFromURL(url) {
    const regex = /(https?:\/\/)?(www\.)?([^\/]+)/;
    const match = url.match(regex);
    if (match && match[3]) {
        return match[3]; // 返回域名部分
    } else {
        return null; // 无效的URL或无法匹配的情况
    }
}
//获取主域名
function getRootDomain(host = "") {
    if (host === "") return ;
    const specialTlds = [
        'com.cn',
        'net.cn',
        'org.cn',
        'gov.cn',
        'co.uk',
        'org.uk'
    ];

    for (const tld of specialTlds) {
        if (host.endsWith('.' + tld)) {
            const parts = host.split('.');
            return parts.slice(-(tld.split('.').length + 1)).join('.');
        }
    }

    const parts = host.split('.');
    return parts.length > 2
        ? parts.slice(-2).join('.')
        : host;
}