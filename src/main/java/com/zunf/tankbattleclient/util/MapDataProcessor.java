package com.zunf.tankbattleclient.util;

import com.google.protobuf.ByteString;
import com.zunf.tankbattleclient.constant.GameConstants;
import com.zunf.tankbattleclient.enums.MapIndex;
import com.zunf.tankbattleclient.protobuf.game.room.GameRoomProto;

import java.util.List;
import java.util.Map;

/**
 * 地图数据处理工具类
 * 负责地图数据的解析、更新和变化检测
 */
public class MapDataProcessor {

    /**
     * 初始化previousMapData（从gameData）
     */
    public static byte[][] initializePreviousMapData(GameRoomProto.StartNotice gameData) {
        if (gameData == null || gameData.getMapDataCount() == 0) {
            return null;
        }

        int mapHeight = gameData.getMapDataCount();
        int mapWidth = gameData.getMapData(0).size();
        byte[][] previousMapData = new byte[mapHeight][mapWidth];

        for (int row = 0; row < mapHeight && row < GameConstants.MAP_SIZE; row++) {
            ByteString rowData = gameData.getMapData(row);
            byte[] rowBytes = rowData.toByteArray();
            for (int col = 0; col < rowBytes.length && col < mapWidth && col < GameConstants.MAP_SIZE; col++) {
                previousMapData[row][col] = rowBytes[col];
            }
        }

        return previousMapData;
    }

    /**
     * 更新地图数据，检测新的已摧毁砖块
     *
     * @param mapDataList 新的地图数据
     * @param previousMapData 上一帧的地图数据
     * @param destroyedBrickAnimations 摧毁动画映射（会被更新）
     * @param isFirstTick 是否是第一个tick
     * @return 更新后的地图数据
     */
    public static byte[][] updateMapData(List<ByteString> mapDataList, byte[][] previousMapData,
                                         Map<String, Long> destroyedBrickAnimations, boolean isFirstTick) {
        if (mapDataList == null || mapDataList.isEmpty()) {
            return previousMapData;
        }

        int mapHeight = mapDataList.size();
        if (mapHeight == 0) {
            return previousMapData;
        }

        int mapWidth = mapDataList.get(0).size();
        byte[][] currentMapData = new byte[mapHeight][mapWidth];

        // 解析当前地图数据
        for (int row = 0; row < mapHeight && row < GameConstants.MAP_SIZE; row++) {
            ByteString rowData = mapDataList.get(row);
            byte[] rowBytes = rowData.toByteArray();
            for (int col = 0; col < rowBytes.length && col < mapWidth && col < GameConstants.MAP_SIZE; col++) {
                currentMapData[row][col] = rowBytes[col];
            }
        }

        // 检测新的已摧毁砖块
        if (previousMapData != null && !isFirstTick) {
            detectDestroyedBricks(previousMapData, currentMapData, mapHeight, mapWidth, destroyedBrickAnimations);
        }

        return currentMapData;
    }

    /**
     * 检测被摧毁的砖块
     */
    private static void detectDestroyedBricks(byte[][] previousMapData, byte[][] currentMapData,
                                               int mapHeight, int mapWidth, Map<String, Long> destroyedBrickAnimations) {
        for (int row = 0; row < mapHeight && row < GameConstants.MAP_SIZE; row++) {
            for (int col = 0; col < mapWidth && col < GameConstants.MAP_SIZE; col++) {
                byte previousType = previousMapData[row][col];
                byte currentType = currentMapData[row][col];

                // 如果之前是可破坏墙(BRICK)，现在是已破坏墙(DESTROYED_WALL)或空地(EMPTY)，触发摧毁动画
                if (previousType == MapIndex.BRICK.getCode() &&
                        (currentType == MapIndex.DESTROYED_WALL.getCode() ||
                                currentType == MapIndex.EMPTY.getCode())) {
                    String key = row + "_" + col;
                    destroyedBrickAnimations.put(key, System.currentTimeMillis());
                }
            }
        }
    }

    /**
     * 更新gameData中的地图数据
     */
    public static GameRoomProto.StartNotice updateGameDataMap(GameRoomProto.StartNotice gameData,
                                                               List<ByteString> mapDataList) {
        if (gameData == null) {
            return null;
        }

        GameRoomProto.StartNotice.Builder builder = gameData.toBuilder();
        builder.clearMapData();
        for (ByteString rowData : mapDataList) {
            builder.addMapData(rowData);
        }
        return builder.build();
    }
}

