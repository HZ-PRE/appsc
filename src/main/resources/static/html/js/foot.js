const appscApi = 'http://127.0.0.1:17140/'
// NODEAPI = 'http://10.10.25.202:8082/'

//以下api
async function getDownloadFile(call) {
	const response = await fetch(`${appscApi}api/${call.api}/${call.type}`, {
		method: 'POST',
		headers: {
			"Content-Type": "application/json",
		},
		body: call["data"]?typeof call["data"] === "string" ?call.data:JSON.stringify(call.data):null
	})
	let result = await response.text();
	if (response.ok && response.status === 200) {
		call.success(result);
	} else {
		call.fail(result);
	}
}
async function getAppConfig(call) {
	const response = await fetch(`${appscApi}api/getConfig?types=${call.types}`, {
		method: "GET",
		headers: {
			"Content-Type": "application/json",
		},
	})
	if (response.ok && response.status === 200) {
		let result = await response.json();
		call?.success(result);
	} else {
		call?.fail(result);
		autolog.error("获取配置失败");
	}
}

//保存配置
async function saveConfig(call) {
	try {
		const response = await fetch(`${appscApi}api/saveConfig`, {
			method: 'POST',
			headers: {
				"Content-Type": "application/json",
			},
			body: JSON.stringify(call.data)
		});
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) {
		call.fail(error);
	}
}
//保存App配置
async function saveFileAppConf(call) {
	try {
		const response = await fetch(`${appscApi}api/saveFileAppConf`, {
			method: 'POST',
			body: call.data
		});
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) {
		call.fail(error);
	}
}
async function getAppVersion(call) {
	const response = await fetch(`${appscApi}api/getAppVersion`, {
		method: "GET",
		headers: {
			"Content-Type": "application/json",
		},
	})
	let result = await response.text();
	if (response.ok && response.status === 200) {
		call.success(result);
	} else {
		autolog.error(result);
	}
}
//去除字符串的特定字符
function removeChar(str, char) {
	// 使用正则表达式替换所有匹配的特定字符为""
	return str.split(char).join("");
}

//不中转，直接上传
async function upload(formData, call) {
	try {
		const response = await fetch(`${appscApi}api/upload`, {
			method: 'POST',
			body: formData
		});
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call(true, result);
		} else {
			call(false, result);
		}
	} catch (error) {
		call(false, error);
	}
}
//获取上传进度
async function updateProgress(call) {
	try {
		const response = await fetch(`${appscApi}api/updateProgress`, {
			method: "GET"
		})
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call(true, result);
		} else {
			call(false, result);
		}
	} catch (error) {
		call(false, error);
	}
}

//不中转，oss直接上传
async function ossUpload(formData, call) {
	try {
		const response = await fetch(`${appscApi}api/ossUpload`, {
			method: 'POST',
			body: formData
		});
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call(true, result);
		} else {
			call(false, result);
		}
	} catch (error) {
		call(false, error);
	}
}
//oss上传
async function ossUploadV1(call) {
	try {
		const response = await fetch(`${appscApi}api/ossUploadV1`, {
			method: 'POST',
			headers: {
				"Content-Type": "application/json",
			},
			body: JSON.stringify(call.data)
		});
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) {
		call.fail(error);
	}
}
//安装包上传回滚
async function updateRollback(formData, call) {
	try {
		const response = await fetch(`${appscApi}api/updateRollback`, {
			method: 'POST',
			body: formData
		});
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call(true, result);
		} else {
			call(false, result);
		}
	} catch (error) {
		call(false, error);
	}
}
//邮件发送
async function senMails(formData, call) {
	try {
		const response = await fetch(`${appscApi}api2/senMail/${formData.get("type")}/${formData.get("importType")}`, {
			method: 'POST',
			body: formData
		});
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call(true, result);
		} else {
			call(false, result);
		}
	} catch (error) {
		call(false, error);
	}
}
//获取密码
async function getPassword(call) {
	try {
		const response = await fetch(`${appscApi}api2/getPassword/${call.type}/${call.version}?text=${call.text}`, {
			method: "GET"
		})
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) {
		call.fail(error);
	}
}
//判断链接是否存在
async function urlCheck(url,call) {
	try {
		const response = await fetch(`${appscApi}api2/urlCheck?url=${url}`, {
			method: "GET"
		})
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call(Number(result));
		} else {
			call(true);
		}
	} catch (error) {
		call(true);
	}
}
//获取节点信息
async function getServerNodes(call) {
	try {
		const response = await fetch(`${NODEAPI}api/getServersNode`, {
			method: "GET"
		})
		let result = await response.json();
		if (response.ok && response.status === 200) {
				call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) {
		call.fail(error);
	}
}
//提交节点信息
async function subServersNode(call) {
	try {
		const response = await fetch(`${NODEAPI}api/createServersNode`, {
			method: 'POST',
			headers: {
			  'Content-Type': 'application/json'
			},
			body: JSON.stringify(call.data)
		});
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		} 
	} catch (error) {
		call.fail(error);
	}
}
//部署节点信息
async function devServersNode(call) { 
	try {
		const response = await fetch(`${NODEAPI}api/SubLinux/${call.t}/${call.id}?app=${call.app}`, {
			method: 'GET'
		});
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) { 
		call.fail(error);
	}
}
//获取部署的节点信息
async function GetServerNodesById(call) { 
	try {
		const response = await fetch(`${NODEAPI}api/GetServerNodesById/${call.id}`, {
			method: 'GET'
		});
		let result = await response.json();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) { 
		call.fail(error);
	}
}
//获取应用部署的节点信息
async function GetServersByApp(call) { 
	try {
		const response = await fetch(`${NODEAPI}api/GetServersByApp/${call.app}`, {
			method: 'GET'
		});
		let result = await response.json();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) { 
		call.fail(error);
	}
}
//获取应用部署的节点信息
async function InitNodeMonitor(call) { 
	try {
		const response = await fetch(`${NODEAPI}api/InitNodeMonitor`, {
			method: 'GET'
		});
		let result = await response.json();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) { 
		call.fail(error);
	}
}
//获取检测链接
async function getServerUrl(call) { 
	try {
		const response = await fetch(`${NODEAPI}api/getServerUrl`, {
			method: 'GET'
		});
		let result = await response.json();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) { 
		call.fail(error);
	}
}
//删除检测链接
async function serverUrlDel(call) { 
	try {
		const response = await fetch(`${NODEAPI}api/ServerUrlDel/${call.id}`, {
			method: 'GET'
		});
		let result = await response.json();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) { 
		call.fail(error);
	}
}
//提交URL信息
async function createServersUrl(call) {
	try {
		const response = await fetch(`${NODEAPI}api/createServersUrl`, {
			method: 'POST',
			headers: {
			  'Content-Type': 'application/json'
			},
			body: JSON.stringify(call.data)
		});
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		} 
	} catch (error) {
		call.fail(error);
	}
}
//获取域名
async function getServerHost(call) {
	try {
		const response = await fetch(`${NODEAPI}api/getServerHost`, {
			method: 'GET'
		});
		let result = await response.json();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) {
		call.fail(error);
	}
}
//根据app获取域名
async function getServersHostByApp(call) {
	try {
		const response = await fetch(`${NODEAPI}api/getServersHostByApp/${call.app}`, {
			method: 'GET'
		});
		let result = await response.json();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) {
		call.fail(error);
	}
}
//删除域名
async function serverHostDel(call) {
	try {
		const response = await fetch(`${NODEAPI}api/ServerHostDel/${call.id}`, {
			method: 'GET'
		});
		let result = await response.json();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) {
		call.fail(error);
	}
}
//提交域名信息
async function createServersHost(call) {
	try {
		const response = await fetch(`${NODEAPI}api/createServersHost`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify(call.data)
		});
		let result = await response.json();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) {
		call.fail(error);
	}
}
//修改域名信息
async function putServerHostByDomain(call) {
	try {
		const response = await fetch(`${NODEAPI}api/putServerHostByDomain`, {
			method: 'PUT',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify(call.data)
		});
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) {
		call.fail(error);
	}
}
//获取字典
async function GetServerDictsByType(call) { 
	try {
		const response = await fetch(`${NODEAPI}api/GetServerDictsByType/${call.t}`, {
			method: 'GET'
		});
		let result = await response.json();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) { 
		call.fail(error);
	}
}
//删除字典
async function ServerDictsDel(call) { 
	try {
		const response = await fetch(`${NODEAPI}api/ServerDictsDel/${call.id}`, {
			method: 'GET'
		});
		let result = await response.json();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) { 
		call.fail(error);
	}
}
//提交字典
async function CreateServerDicts(call) {
	try {
		const response = await fetch(`${NODEAPI}api/CreateServerDicts`, {
			method: 'POST',
			headers: {
			  'Content-Type': 'application/json'
			},
			body: JSON.stringify(call.data)
		});
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		} 
	} catch (error) {
		call.fail(error);
	}
}
//获取在线文件内容
async function urlOnLineFile(call) {
	try {
		const response = await fetch(call.url, {
			method: "GET"
		})
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		} 
	} catch (error) {
		call.fail(error);
	}
}
//获取授权API
async function getServerSupplierApiBySupplier(call) {
	try {
		const response = await fetch(`${NODEAPI}api/getServerSupplierApiBySupplier/${call.supplier}/${call.cdn}`, {
			method: 'GET'
		});
		let result = await response.json();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) {
		call.fail(error);
	}
}
//获取授权API
async function getServerSupplierApi(call) {
	try {
		const response = await fetch(`${NODEAPI}api/getServerSupplierApi`, {
			method: 'GET'
		});
		let result = await response.json();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) {
		call.fail(error);
	}
}
//删除授权API
async function serverSupplierApiDel(call) {
	try {
		const response = await fetch(`${NODEAPI}api/serverSupplierApiDel/${call.id}`, {
			method: 'DELETE'
		});
		let result = await response.json();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) {
		call.fail(error);
	}
}
//提交授权API
async function postServerSupplierApi(call) {
	try {
		const response = await fetch(`${NODEAPI}api/postServerSupplierApi`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json'
			},
			body: JSON.stringify(call.data)
		});
		let result = await response.text();
		if (response.ok && response.status === 200) {
			call.success(result);
		} else {
			call.fail(result);
		}
	} catch (error) {
		call.fail(error);
	}
}
//------------api结束----------

//-------其它方法
//是否是纯数字
function isPureNumber(str) {
	return /^[0-9]+$/.test(str);
}
