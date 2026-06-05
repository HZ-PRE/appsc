var NODEAPI = 'http://10.10.25.202:8082/'
// 本次更新的版本
const appVersion = 277;

// 本次更新说明
const appVersionTxt = "域名管理自动化功能bug修复";

const APPCONFSUM=10;

const APPACCESSKETLIST= ['lxf','mxh','suzijie','yangzhongjie','liuyisen','gongpenglong','huangshuiping','yangjiaming','chengyihao','jiajunhao','yangjing','yangzhicheng','hw'];

const COUNTRIESCN = [
    "香港","澳门","台湾","中国","美国","日本","韩国","朝鲜","俄罗斯",
    "英国","法国","德国","意大利","西班牙","葡萄牙",
    "荷兰","比利时","瑞士","奥地利","瑞典","挪威","丹麦","芬兰",
    "加拿大","澳大利亚","新西兰",
    "印度","巴基斯坦","孟加拉国","斯里兰卡","尼泊尔","不丹",
    "泰国","越南","老挝","柬埔寨","缅甸","马来西亚","新加坡","印度尼西亚","菲律宾",
    "沙特阿拉伯","阿联酋","卡塔尔","科威特","伊朗","伊拉克","以色列","土耳其",
    "埃及","利比亚","阿尔及利亚","摩洛哥","突尼斯","南非","尼日利亚","肯尼亚","埃塞俄比亚",
    "巴西","阿根廷","智利","秘鲁","哥伦比亚","墨西哥","委内瑞拉",
    "乌克兰","白俄罗斯","波兰","捷克","匈牙利","希腊","罗马尼亚","保加利亚","塞尔维亚","克罗地亚",
    "阿尔巴尼亚","斯洛伐克","斯洛文尼亚","立陶宛","拉脱维亚","爱沙尼亚",
    "哈萨克斯坦","乌兹别克斯坦","吉尔吉斯斯坦","塔吉克斯坦","土库曼斯坦",
    "蒙古","阿富汗","叙利亚","黎巴嫩","约旦","也门","阿曼"
];
const SUPPLIERMAP = {
    ali: '阿里云',
    ucloud: '优刻云',
    huawei: '华为云',
    cloudflare: '泛播',
    tencent: '腾讯云',
    baidu: '百度云',
    qiniu: '七牛云',
    jdcloud: '京东云',
    namesilo: 'namesilo',
};
const FUHAO=['+',','];

const HOSTOSS = [
    "https://pubtofile.oss-cn-hongkong.aliyuncs.com/",
    "https://raw.githubusercontent.com/ym-source/ym/main/",
    "https://yingmao.oss-cn-hongkong.aliyuncs.com/",
    "https://wfjsq.s3.ap-east-1.amazonaws.com/",
    "https://yingmao2.s3.ap-east-1.amazonaws.com/",
    "https://pub-eed78fedcfb6470ea94589a3771b4e0f.r2.dev/"
]

const APPKFTYPE=[
    {
        name: "广州客服",
        val:'gz_kf',
        host: "gz_kefu.txt",
        origin:"kf.yingmaox.cc"
    },
    {
        name: "广州客服2",
        val:'gz_kf',
        host:"kf/gz_kefu.txt",
        origin:"kf.yingmaox.cc"
    },
    {
        name: "湖北客服",
        val: 'gz_kf',
        host: "kf/hb_kefu.txt",
        origin:"kf.yingmaox.cc"
    },
    {
        name: "海外客服",
        val: 'hw_kf',
        host: "hw_kefu.txt",
        origin:""
    }
]

const APPTYPENAME = [
    {
        name: "影猫",
        val: "ym",
        host: "host_ym.txt",
        origin:"api.yingmaox.cc"
    }, {
        name: "太子",
        val: "tz",
        host: "hosttaizi.txt",
        origin:"tz.tztoym.com"
    }, {
        name: "八爪鱼",
        val: "bzy",
        host: "hostdef.txt",
        origin:"bzyapi.bazhuyuvpn.cc"
    }, {
        name: "云梯",
        val: "yt",
        host: "host_yunti.txt",
        origin:"api1.ytsjpro.com"
    }, {
        name: "马上连",
        val: "msl",
        host: "host_wf.txt",
        origin:"api.wfvpnpro.com"
    }, {
        name: "一键连",
        val: "yjl",
        host: "host_yjl.txt",
        origin:"api.yijianlianjsq.com"
    }, {
        name: "橘猫",
        val: "jm",
        host: "host_jm.txt",
        origin:"api.jumaoniubi.com"
    }, {
        name: "二键连",
        val: "ejl",
        host: "host_yjlsecond.txt",
        origin:"api2.yijianlianjsq.com"
    }, {
        name: "橘猫2",
        val: "jm2",
        host: "host_jm2.txt",
        origin:"api.lanniaoo.com"
    }, {
        name: "三键连",
        val: "sjl",
        host: "host_yjl3.txt",
        origin:"api3.onetouchjsq.com"
    }, {
        name: "网飞",
        val: "wf",
        host: "host_wf.txt",
        origin:"api.wfvpnpro.com"
    }, {
        name: "行云",
        val: "xy",
        host: "host_xy.txt",
        origin:"api.xingyunnb.com"
    }, {
        name: "超飞",
        val: "cf",
        host: "host_chaofei.txt",
        origin:""
    }, {
        name: "好易联",
        val: "hyl",
        host: "host_hyl.txt",
        origin:"api.haoyilian.vip"
    },{
        name: "影喵",
        val: "ymm",
        host: "host_xy.txt",
        origin:""
    }, {
        name: "快飞",
        val: "bsm",
        host: "host_kuaifei.txt",
        origin:"api.huiguovpn.win:18008"
    }, {
        name: "极捷",
        val: "jj",
        host: "host_jijie.txt",
        origin:""
    }, {
        name: "蓝鹰",
        val: "ly",
        host: "host_lanying.txt",
        origin:""
    }, {
        name: "星域",
        val: "xinyu",
        host: "host_xy.txt",
        origin:""
    },{
        name: "即刻连",
        val: "jkl",
        host: "host_jkl.txt",
        origin:""
    }
]