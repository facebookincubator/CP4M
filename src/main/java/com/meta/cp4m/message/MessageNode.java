/*
 *
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.cp4m.message;

public class MessageNode <T extends Message>{
    T message;
    T parentMessage;

    public MessageNode(T message){
        this.message = message;
        this.parentMessage = null;
    }
    public MessageNode(T message, T parentMessage){
        this.message = message;
        this.parentMessage = parentMessage;
    }
    public T getMessage() {
        return message;
    }
    public T getParentMessage() {
        return parentMessage;
    }
    public void setMessage(T message) {
        this.message = message;
    }
    public void setParentMessage(T parentMessage) {
        this.parentMessage = parentMessage;
    }
}
