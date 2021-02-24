package ru.hlf_gaz.ppe_smart_contract.chaincode;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;

@Contract(
        name = "PPE_Smart_Contract",
        info = @Info(
                title = "Asset Transfer",
                description = "The hyperlegendary asset transfer for Gazprom's PPE",
                version = "0.0.1-SNAPSHOT",
                contact = @Contact(
                        email = "Shmendyuk.NV@gazprom-neft.ru")))
@Default
public final class AssetTransfer implements ContractInterface {

    private final Genson genson = new Genson();

    private enum AssetTransferErrors {
        ASSET_NOT_FOUND,
        ASSET_ALREADY_EXISTS
    }

    /**
     * Creates some initial assets on the ledger.
     *
     * @param ctx the transaction context
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void InitTestLedger(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        CreateAsset(ctx, "asset1", "Иван Иванов", "001",
                "БЕЛЬЕ НАТ. ТРИКОТАЖ. 60/62 182/188", 182.85F, "720000011068",
                "10.02.2021", 20, "ГНП-Д1", "none");
        CreateAsset(ctx, "asset2", "Петр Петрович", "002",
                "БОТИНКИ КОЖ ВЫС БЕРЦЕМ МЕТ ПОДНОСКОМ 45", 478.16F, "720000010964",
                "15.02.2021", 15, "ГНП-Д2", "none");

    }

    /**
     * Creates a new asset on the ledger.
     *
     * @param ctx the transaction context
     * @param assetID the ID of the new asset
     * @param ownerName the employee name
     * @param ownerID employee's personnel number
     * @param name PPE name
     * @param price PPE price
     * @param inventoryNumber PPE inventory number in subsidiary
     * @param startUseDate a date when PPE using has been started
     * @param lifeTime a life time to use PPE
     * @param subsidiary a company owned PPE / company where the employee is located
     * @param prevSubsidiary a company where employee were previously
     * @return the created asset
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Asset CreateAsset(final Context ctx, final String assetID, final String ownerName,
                             final String ownerID, final String name,
                             final Float price, final String inventoryNumber,
                             final String startUseDate, final Integer lifeTime,
                             final String subsidiary, final String prevSubsidiary) {
        ChaincodeStub stub = ctx.getStub();

        if (AssetExists(ctx, assetID)) {
            String errorMessage = String.format("Asset %s already exists", assetID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_ALREADY_EXISTS.toString());
        }

        Asset asset = new Asset(assetID, ownerName, ownerID, name, price, inventoryNumber, startUseDate, lifeTime, subsidiary, prevSubsidiary);
        String assetJSON = genson.serialize(asset);
        stub.putStringState(assetID, assetJSON);

        return asset;
    }

    /**
     * Retrieves an asset with the specified ID from the ledger.
     *
     * @param ctx the transaction context
     * @param assetID the ID of the asset
     * @return the asset found on the ledger if there was one
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Asset ReadAsset(final Context ctx, final String assetID) {
        ChaincodeStub stub = ctx.getStub();
        String assetJSON = stub.getStringState(assetID);

        if (assetJSON == null || assetJSON.isEmpty()) {
            String errorMessage = String.format("Asset %s does not exist", assetID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        Asset asset = genson.deserialize(assetJSON, Asset.class);
        return asset;
    }

    /**
     * Updates the properties of an asset on the ledger.
     *
     * @param ctx the transaction context
     * @param assetID the ID of the asset being updated
     * @param ownerID employee's personnel number being updated
     * @param price PPE price being updated
     * @param inventoryNumber PPE inventory number in subsidiary being updated
     * @param subsidiary a company owned PPE / company where the employee is located being updated
     * @param prevSubsidiary a company where employee were previously being updated
     * @return the transferred asset
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Asset UpdateAsset(final Context ctx, final String assetID, final String ownerName,
                             final String ownerID, final String name,
                             final Float price, final String inventoryNumber,
                             final String startUseDate, final Integer lifeTime,
                             final String subsidiary, final String prevSubsidiary) {
        ChaincodeStub stub = ctx.getStub();

        if (!AssetExists(ctx, assetID)) {
            String errorMessage = String.format("Asset %s does not exist", assetID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        Asset newAsset = new Asset(assetID, ownerName, ownerID, name, price, inventoryNumber, startUseDate, lifeTime, subsidiary, prevSubsidiary);
        String newAssetJSON = genson.serialize(newAsset);
        stub.putStringState(assetID, newAssetJSON);
        System.out.println("Update with: " + newAsset.toString());

        return newAsset;
    }

    /**
     * Deletes asset on the ledger.
     *
     * @param ctx the transaction context
     * @param assetID the ID of the asset being deleted
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void DeleteAsset(final Context ctx, final String assetID) {
        ChaincodeStub stub = ctx.getStub();

        if (!AssetExists(ctx, assetID)) {
            String errorMessage = String.format("Asset %s does not exist", assetID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        System.out.println("Asset {" + stub.getStringArgs().toString() + "} deleted");
        stub.delState(assetID);
    }

    /**
     * Checks the existence of the asset on the ledger
     *
     * @param ctx the transaction context
     * @param assetID the ID of the asset
     * @return boolean indicating the existence of the asset
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean AssetExists(final Context ctx, final String assetID) {
        ChaincodeStub stub = ctx.getStub();
        String assetJSON = stub.getStringState(assetID);

        return (assetJSON != null && !assetJSON.isEmpty());
    }

    /**
     * Changes the owner of a asset on the ledger.
     *
     * @param ctx the transaction context
     * @param assetID the ID of the asset being transferred
     * @param newOwnerID new employee's personnel number for another company
     * @param newInventoryNumber new inventory number for another company
     * @param fromSubsidiary the company from which the employee is transferred
     * @param toSubsidiary the company to which the employee is transferred
     * @return the updated asset
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Asset TransferAsset(final Context ctx, final String assetID,
                               final String newOwnerID, final String newInventoryNumber,
                               final String fromSubsidiary, final String toSubsidiary) {
        ChaincodeStub stub = ctx.getStub();
        String assetJSON = stub.getStringState(assetID);

        if (assetJSON == null || assetJSON.isEmpty()) {
            String errorMessage = String.format("Asset %s does not exist", assetID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        Asset asset = genson.deserialize(assetJSON, Asset.class);

        Asset newAsset = new Asset(asset.getAssetID(), asset.getOwnerName(), newOwnerID, asset.getName(), asset.getPrice(),
                newInventoryNumber, asset.getStartUseDate(), asset.getLifeTime(), toSubsidiary, fromSubsidiary);
        String newAssetJSON = genson.serialize(newAsset);
        stub.putStringState(assetID, newAssetJSON);

        return newAsset;
    }

    /**
     * Retrieves all assets from the ledger.
     *
     * @param ctx the transaction context
     * @return array of assets found on the ledger
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetAllAssets(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        List<Asset> queryResults = new ArrayList<Asset>();

        // To retrieve all assets from the ledger use getStateByRange with empty startKey & endKey.
        // Giving empty startKey & endKey is interpreted as all the keys from beginning to end.
        // As another example, if you use startKey = 'asset0', endKey = 'asset9' ,
        // then getStateByRange will retrieve asset with keys between asset0 (inclusive) and asset9 (exclusive) in lexical order.
        QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "");

        for (KeyValue result: results) {
            Asset asset = genson.deserialize(result.getStringValue(), Asset.class);
            queryResults.add(asset);
            System.out.println(asset.toString());
        }

        final String response = genson.serialize(queryResults);

        return response;
    }
}
