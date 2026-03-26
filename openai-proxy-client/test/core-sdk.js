/**
 * Demo: Use Primus zkTLS to get BTC price from Binance
 *
 * This script demonstrates how to use @primuslabs/zktls-core-sdk to make
 * a verifiable HTTPS request to Binance and extract the BTC price with a proof.
 */
const path = require('path');
const {PrimusCoreTLS} = require('@primuslabs/zktls-core-sdk');
require('dotenv').config({path: path.join(__dirname, '..', '.env')});

const ZKTLS_APP_ID = process.env.ZKTLS_APP_ID || process.env.PRIMUS_APP_ID;
const ZKTLS_APP_SECRET = process.env.ZKTLS_APP_SECRET || process.env.PRIMUS_APP_SECRET;
const ZKTLS_INIT_MODE = process.env.ZKTLS_INIT_MODE || 'auto';

if (!ZKTLS_APP_ID || !ZKTLS_APP_SECRET) {
    console.error('Error: ZKTLS_APP_ID and ZKTLS_APP_SECRET (or PRIMUS_APP_*) are required');
    console.error('Please set these in your .env file');
    process.exit(1);
}

/**
 * Parse attestation result field
 */
function parseAttestedField(attestation, keyName) {
    let data = attestation.data;
    if (typeof data === 'string') {
        try {
            data = JSON.parse(data);
        } catch {
            return null;
        }
    }
    if (!data || typeof data !== 'object') {
        return null;
    }
    const raw = data[keyName];
    if (raw === undefined) {
        return null;
    }
    if (typeof raw === 'string') {
        try {
            return JSON.parse(raw);
        } catch {
            return raw;
        }
    }
    return raw;
}

/**
 * Main function to get BTC price from Binance with zkTLS proof
 */
async function getBtcPrice() {
    console.log('='.repeat(60));
    console.log('Primus zkTLS - Binance BTC Price Demo');
    console.log('='.repeat(60));

    try {
        // Step 1: Initialize PrimusCoreTLS
        console.log('\n[1] Initializing Primus zkTLS...');
        const zkTLS = new PrimusCoreTLS();
        await zkTLS.init(ZKTLS_APP_ID, ZKTLS_APP_SECRET, ZKTLS_INIT_MODE);
        console.log('    ✓ Primus zkTLS initialized');

        // Step 2: Build request to Binance API
        console.log('\n[2] Building request to Binance...');
        const request = {
            url: 'https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT',
            method: 'GET',
            header: {
                accept: 'application/json'
            },
            body: ''
        };
        console.log(`    URL: ${request.url}`);

        // Step 3: Build response extraction rules
        console.log('\n[3] Building response extraction rules...');
        const responseResolves = [
            {
                keyName: 'symbol',
                parseType: 'json',
                parsePath: '$.symbol'
            },
            {
                keyName: 'price',
                parseType: 'json',
                parsePath: '$.price'
            }
        ];
        console.log(`    Extracting fields: symbol, price`);

        // Step 4: Generate request params and start attestation
        console.log('\n[4] Generating attestation request...');
        const attRequest = zkTLS.generateRequestParams(request, responseResolves);
        attRequest.setAttMode({
            algorithmType: 'proxytls',
            resultType: 'plain'
        });
        console.log('    ✓ Request parameters generated');

        console.log('\n[5] Starting attestation (this may take a while)...');
        const startTime = Date.now();
        const urls = {
            primusMpcUrl: "wss://api-dev.padolabs.org/algorithm-0g",
            primusProxyUrl: "wss://api-dev.padolabs.org/algorithm-proxy-0g",
            proxyUrl: "wss://api-dev.padolabs.org/algoproxy-0g"
        }
        const attestation = await zkTLS.startAttestation(attRequest, 2 * 60 * 1000, urls);
        // const attestation = await zkTLS.startAttestation(attRequest, 2 * 60 * 1000);
        const duration = Date.now() - startTime;
        console.log(`    ✓ Attestation completed (${duration}ms)`);

        // Step 5: Verify attestation (optional, for demo)
        console.log('\n[6] Verifying attestation...');
        const isValid = await zkTLS.verifyAttestation(attestation);
        console.log(`    ${isValid ? '✓' : '✗'} Attestation verification: ${isValid ? 'VALID' : 'INVALID'}`);

        // Step 6: Extract data from attestation
        console.log('\n[7] Extracting data from attestation...');
        const symbol = parseAttestedField(attestation, 'symbol');
        const price = parseAttestedField(attestation, 'price');

        if (symbol && price) {
            console.log('    ✓ Data extracted successfully');
            console.log('\n' + '='.repeat(60));
            console.log('RESULT');
            console.log('='.repeat(60));
            console.log(`Symbol: ${symbol}`);
            console.log(`Price:  ${price} USDT`);
            console.log('='.repeat(60));

            console.log('\nATTESTATION (proof):');
            console.log(JSON.stringify(attestation, null, 2));
        } else {
            console.error('    ✗ Failed to extract data from attestation');
            console.error('    Attestation data:', attestation.data);
        }
    } catch (error) {
        console.error('\n✗ Error occurred:', error.message);
        if (error.code) {
            console.error(`   Error code: ${error.code}`);
        }
        if (error.stack) {
            console.error('\nStack trace:');
            console.error(error.stack);
        }
        process.exit(1);
    }
}

// Run the demo
getBtcPrice();
