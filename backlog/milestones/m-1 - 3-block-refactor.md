---
id: m-1
title: "3-Block Refactor"
---

## Description

Refactor the single RedstoneChainBlock into three specialized block types (Input, Output, Connector) to simplify signal propagation. Input blocks detect external signals, Output blocks emit signals, Connector blocks are passive routing nodes. Legacy RedstoneChainBlock is kept for backward compatibility.
