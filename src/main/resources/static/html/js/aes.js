function strToUint8(str) {
	return new TextEncoder().encode(str); // UTF-8
}

function uint8ToStr(buf) {
	return new TextDecoder("utf-8").decode(buf);
}

function bytesToHex(buffer) {
	return Array.from(new Uint8Array(buffer))
		.map(b => b.toString(16).padStart(2, "0"))
		.join("");
}

function hexToBytes(hex) {
	if (hex.length % 2 !== 0) {
		throw new Error("HEX长度必须是偶数");
	}

	const bytes = new Uint8Array(hex.length / 2);

	for (let i = 0; i < hex.length; i += 2) {
		bytes[i / 2] = parseInt(hex.slice(i, i + 2), 16);
	}

	return bytes;
}

async function importAesKey128(key) {
	const keyBytes = strToUint8(key);

	if (keyBytes.length !== 16) {
		throw new Error("AES-128 密钥必须是16字节");
	}

	return crypto.subtle.importKey(
		"raw",
		keyBytes, {
			name: "AES-CBC"
		},
		false,
		["encrypt", "decrypt"]
	);
}

// 加密：UTF-8 明文 -> HEX 密文
async function aes128CbcEncryptToHex(plainText, key, iv) {
	const keyObj = await importAesKey128(key);
	const ivBytes = strToUint8(iv);

	if (ivBytes.length !== 16) {
		throw new Error("IV必须是16字节");
	}

	const encrypted = await crypto.subtle.encrypt({
			name: "AES-CBC",
			iv: ivBytes
		},
		keyObj,
		strToUint8(plainText)
	);

	return bytesToHex(encrypted);
}

// 解密：HEX 密文 -> UTF-8 明文
async function aes128CbcDecryptFromHex(cipherHex, key, iv) {
	const keyObj = await importAesKey128(key);
	const ivBytes = strToUint8(iv);

	if (ivBytes.length !== 16) {
		throw new Error("IV必须是16字节");
	}

	const decrypted = await crypto.subtle.decrypt({
			name: "AES-CBC",
			iv: ivBytes
		},
		keyObj,
		hexToBytes(cipherHex)
	);

	return uint8ToStr(decrypted);
}