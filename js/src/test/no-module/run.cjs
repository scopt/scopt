const fs = require("node:fs");
const vm = require("node:vm");

const bundle = fs.readFileSync(process.argv[2], "utf8");
const context = vm.createContext({});
vm.runInContext(bundle, context, { filename: process.argv[2], timeout: 10000 });
vm.runInContext("checkParser()", context, { timeout: 10000 });
