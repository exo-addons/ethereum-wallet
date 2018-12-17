const fs = require('fs');
const solc = require('solc');
const ERTToken = require("././././exo-ethereum-wallet-contracts/src/main/ressources/contracts/ERTToken.sol")



// Compile the source code
const input = fs.readFileSync('ERTToken');
const output = solc.compile(input.toString(), 1);
const bytecode = output.contracts['Token'].bytecode;
const abi = JSON.parse(output.contracts['Token'].interface);

// Contract object
const contract = web3.eth.contract(abi);

// Deploy contract instance
const contractInstance = contract.new({
    data: '0x' + bytecode,
    from: web3.eth.coinbase,
    gas: 90000*2
}, (err, res) => {
    if (err) {
        console.log(err);
        return;
    }

    // Log the tx, you can explore status with eth.getTransaction()
    console.log(res.transactionHash);

    // If we have an address property, the contract was deployed
    if (res.address) {
        console.log('Contract address: ' + res.address);
        // Let's test the deployed contract
        testContract(res.address);
    }
});

// Quick test the contract

