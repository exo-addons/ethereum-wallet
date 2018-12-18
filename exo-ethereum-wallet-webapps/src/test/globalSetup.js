module.exports = async () => {
  const ganache = require("ganache-cli");
  global.server = ganache.server({
    "network_id": 4452365,
    "gasPrice": "0x77359400", // 2 GWei
    "gasLimit": "0x989680", // 10 000 000
    "accounts" : [
      {
         "secretKey":"0x40899e93e69cf79747e4030a2be582e23faac3ccbdf81de602bafd1e531f379f",
         "address":"0x2d232d448FB0B5b370D3abAD2681399e2002aE2A",
         "balance": "100000000000000000000" // 100 eth
      },
      {
         "secretKey":"0x9b2b566b5b9eec0e21a559da10b3d8545f1037e239e128b9cd980c9580fcd949",
         "address":"0xb460A021b66A1f421970B07262Ed11d626B798EF",
         "balance": "100000000000000000000"
      },
      {
         "secretKey":"0x22c11f85efdc6c0c365e15ff3892e59c4856110aa9adce48481318a433e9d16e",
         "address":"0x0a6B396f8EB23CDaf2dB137410Ff7C1f20bBBc57",
         "balance": "100000000000000000000"
      },
      {
         "secretKey":"0x18bc5095bf56b8513d70a3d145e9485371100cb7bd3ff4b0a9d99b1b8b15d624",
         "address":"0xf8622910fa3C83d9A5b22C5e5F00d719A93DEB38",
         "balance": "100000000000000000000"
      },
      {
         "secretKey":"0x7faca2b78ed01cbacb188c4473cb6cfa2edd74d51229cb2266c46c220b27cab8",
         "address":"0xc48AEE6064444D6a62b0fD41f79818B3a77e3ab9",
         "balance": "100000000000000000000"
      },
      {
         "secretKey":"0xd17de90ca3bd4ddf87f10053bea977e395c3ba4959fddb6b1d7daa1c9c89ed26",
         "address":"0x1C970755CaC148a89C01F39B3a148199fC4B8329",
         "balance": "100000000000000000000"
      },
      {
         "secretKey":"0x688e4b89b5daf7cb3fc991e0f739c3cabc64dbdc8c558aac579345543888c247",
         "address":"0x79032C0930f9cEb7546946eBc94e0cb00732C8a7",
         "balance": "100000000000000000000"
      },
      {
         "secretKey":"0xedf8ba07c6816ffb136c04ed0ad337e0994b8d0e8cc7c8aa7092b9f065ec466b",
         "address":"0xb64F82a96569F932Aa7Cb9B997E73E6B3B299cCF",
         "balance": "100000000000000000000"
      },
      {
         "secretKey":"0xaad16f2c4cac7b45a80434db6554af6866f0e8935020f3c9d75a2be6b48e6751",
         "address":"0x5743fE9068772006C9e337917dFE493db8207F6C",
         "balance": "100000000000000000000"
      },
      {
         "secretKey":"0x817f258f52af8741f1082ad1f46bbd6caa87947e547eb13965dfad47b10cb803",
         "address":"0x8f651bD0238E9515612fcB1b668ddBc70894E3F1",
         "balance": "100000000000000000000"
      }
    ]
  });
  global.adminAccount = "0x2d232d448FB0B5b370D3abAD2681399e2002aE2A";
  global.server.listen(8545);
};
