`define CACHE_EVENT_BIT_WIDTH 5
`define CACHE_STATE_BIT_WIDTH 5
`define CACHE_CTR_STATE_BIT_WIDTH 5
`define BROADCAST_TYPE_WIDTH 5
`define VALID 1'b1
`define INVALID 1'b0

typedef enum logic[4:0] {
    NONE_EVENT,
    GETS,
    GETM,
    PUTM,
    UPG,
    MEMORY_DATA,
    MEMORY_ACK,
    INVALIDATE
} event_t;

typedef enum logic[4:0] {
    NONE_CACHE_EVENT,
    //core events
    LOAD,
    STORE,
    AMO,
    //snoop events
    OWN_GETS, // 4
    OWN_GETM,
    OTHER_GETS,
    OTHER_GETM,
    OWN_PUTM,
    OTHER_PUTM,
    OWN_UPG,
    OTHER_UPG,
    // INV,
    //resp events
    DATA, // 12
    ACK,
    // NACK,
    FLUSH,
    LLCC_FLUSH
} cache_event_t;


typedef enum logic[4:0] {
    IDLE, // 0
    COHERENCE_STATE_MACHINE, // 1
    ADD_TO_WB_QUEUE, // 2
    ADD_TO_REQ_QUEUE, // 3
    ADD_TO_CACHE_RESP_QUEUE, // 4
    ADD_TO_CACHE_RESP_QUEUE_STORE, // 5
    ADD_TO_CACHE_RESP_AND_WB_QUEUE, // 6
    ADD_TO_CACHE_SNOOP_RESP_QUEUE, // 7
    ADD_TO_TAG_ARRAY, // 8
    UPDATE_CACHE, // 9
    REFILL_CACHE, // 10
    IDLE_2, // 11
    IDLE_3,
    WRITE_TO_CACHE, // 13
    //mem cache controller aditional state
    ADD_TO_MEMORY_QUEUE, // 14
    ADD_TO_PR_LOOKUP_TABLE, // 15
    ADD_TO_MEM_RESP, // 16
    WAKEUP_DEP,
    ADD_TO_MEM_RESP_2,
    WRITE_BACK,
    REMOVE_FROM_CACHE,
    CACHE_FLUSH,
    CACHE_FLUSH_UPDATE,
    CACHE_FLUSH_WAIT,
    ADD_FLUSH_TO_WB_QUEUE,
    LLCC_FLUSH_DONE,
    WRITE_DATA_ARRAY_ADD_TO_CACHE_RESP_Q,
    READ_DATA_ARRAY,

    LOOKUP_TABLE_REQ_WR_WAIT,
    LOOKUP_TABLE_REQ_RD_WAIT,
    ADD_TO_MEMORY_QUEUE_2
    //,
    // ADD_TO_CACHE_RESP_QUEUE_STORE_AMO,
    // WRITE_TO_CACHE_AMO
} cc_state_t;

typedef enum logic[4:0] {
    I,
    S,
    M,

    IS_AD,
    IS_D,
    IS_DI,
    IS_UI,

    IM_AD,
    IM_D,
    IM_A,
    // IM_DS,
    IM_DI,
    IM_DSI,

    IM_DUI, //
    IM_UI, //
    IM_DUS, //
    IM_US, //

    IM_AD_AMO, //Atomic request transient state
    IM_D_AMO, //Atomic request transient state
    IM_DI_AMO,
    MI_AMO,
    M_AMO,
    // M_I,
    MI_A,
    // MI_AU, //intermediate state (between IM_DI to MI_A) for Refill to a cache line is done, but store to the line is not done
    MS_A,
    // MS_AU, //intermediate state (between IM_DS to MS_A) for Refill to a cache line is done, but store to the line is not done
    // II_A,
    // I_I,

    SM_W,
    IM_W,
    // MS_WB,
    // MI_WB,
    MM,

    //mem transient coherence state
    IS, ////"Intermediate State I>S";
    IM, //"Intermediate State I>M";
    MI, //"Intermediate State M>I";
    II,
    II_D
} tbe_state_t;

module coherence_table (
                        input                                         clock,
                        input                                         reset_i ,
                        input [ `CACHE_EVENT_BIT_WIDTH-1:0]           cache_line_event_i ,
                        input [ `CACHE_STATE_BIT_WIDTH-1:0]           cache_line_state_i ,
                        input                                         cache_line_tag_match_i ,
                        input                                         cache_line_dirty_i ,
                        input                                         cache_line_valid_i ,
                        output wire [`CACHE_CTR_STATE_BIT_WIDTH-1:0] cache_ctr_next_state_o ,
                        output wire [ `CACHE_STATE_BIT_WIDTH-1:0]    cache_line_next_state_o ,
                        output wire                                  cache_line_next_dirty_o ,
                        output wire                                  cache_line_next_valid_o ,
                        output wire [ `BROADCAST_TYPE_WIDTH-1:0]     cache_line_broadcast_type
                        );

  always @(posedge clock) begin
     //$display("cache_line_event_i %d cache_line_state_i %d cache_line_next_state_o %d cache_ctr_next_state_o %d", cache_line_event_i, cache_line_state_i, cache_line_next_state_o, cache_ctr_next_state_o);
   end

   always @(*) begin
      cache_ctr_next_state_o    = {`CACHE_CTR_STATE_BIT_WIDTH{1'b0}};
      cache_line_next_state_o   = {`CACHE_STATE_BIT_WIDTH{1'b0}};
      cache_line_next_dirty_o   = '0;
      cache_line_next_valid_o   = '0;
      cache_line_broadcast_type = NONE_EVENT;
      if(reset_i) begin
      end else begin
         case (cache_line_event_i)
           LOAD : begin
              case (cache_line_state_i)
                M : begin
                   if(cache_line_tag_match_i) begin
                      cache_line_next_state_o = M;
                      cache_ctr_next_state_o  = ADD_TO_CACHE_RESP_QUEUE;
                      cache_line_next_valid_o = 1'b1;
                      cache_line_next_dirty_o = cache_line_dirty_i;
                   end else begin //potentially modified so write it back
                      cache_line_next_state_o = MI_A; //replacement
                      cache_ctr_next_state_o  = ADD_TO_WB_QUEUE;
                      cache_line_next_valid_o = 1'b1;
                      cache_line_next_dirty_o = cache_line_dirty_i;
                      cache_line_broadcast_type = PUTM;
                   end
                end
                S : begin
                   if(cache_line_tag_match_i) begin
                      cache_line_next_state_o = S;
                      cache_ctr_next_state_o  = ADD_TO_CACHE_RESP_QUEUE;
                      cache_line_next_valid_o = 1'b1;
                      cache_line_next_dirty_o = cache_line_dirty_i;
                   end else begin
                      cache_line_next_state_o   = IS_AD;
                      cache_ctr_next_state_o    = ADD_TO_REQ_QUEUE;
                      cache_line_next_valid_o   = 1'b1;
                      cache_line_broadcast_type = GETS;
                   end // end else
                end
                IS_UI : begin
                   if(cache_line_tag_match_i) begin
                      cache_line_next_state_o = I;
                      cache_ctr_next_state_o  = ADD_TO_CACHE_RESP_QUEUE;
                      cache_line_next_valid_o = 1'b1;
                      cache_line_next_dirty_o = cache_line_dirty_i;
                    end
                end
                I : begin
                   cache_line_next_state_o   = IS_AD;
                   cache_ctr_next_state_o    = ADD_TO_REQ_QUEUE;
                   cache_line_next_valid_o   = 1'b1;
                   cache_line_broadcast_type = GETS;
                end
                default:;
              endcase
           end // LOAD:
           STORE : begin
              case (cache_line_state_i)
                M : begin
                   if(cache_line_tag_match_i) begin
                      cache_line_next_state_o = M;
                      cache_ctr_next_state_o  = ADD_TO_CACHE_RESP_QUEUE_STORE;
                      cache_line_next_valid_o = 1'b1;
                      cache_line_next_dirty_o = '1;
                   end else begin //potentially modified so write it back
                      cache_line_next_state_o = MI_A; //replacement
                      cache_ctr_next_state_o  = ADD_TO_WB_QUEUE;
                      cache_line_next_valid_o = 1'b1;
                      cache_line_next_dirty_o = cache_line_dirty_i;
                      cache_line_broadcast_type = PUTM;
                   end
                end
                IM_UI : begin //MI_AU
                   if(cache_line_tag_match_i) begin
                      cache_line_next_state_o = MI_A;
                      cache_ctr_next_state_o  = WRITE_DATA_ARRAY_ADD_TO_CACHE_RESP_Q; //ADD_TO_WB_QUEUE;
                      cache_line_next_valid_o = 1'b1;
                      cache_line_next_dirty_o = '1;
                      cache_line_broadcast_type = PUTM;
                   end else begin
                   end
                end
                IM_US : begin //MI_AU
                   if(cache_line_tag_match_i) begin
                      cache_line_next_state_o = MS_A; //MS_AU;
                      cache_ctr_next_state_o  = WRITE_DATA_ARRAY_ADD_TO_CACHE_RESP_Q;//ADD_TO_WB_QUEUE;
                      cache_line_next_valid_o = 1'b1;
                      cache_line_next_dirty_o = '1;
                      cache_line_broadcast_type = PUTM;
                   end else begin
                   end
                end
                S : begin
                   if(cache_line_tag_match_i) begin
                      cache_line_next_state_o   = SM_W;
                      cache_ctr_next_state_o    = ADD_TO_REQ_QUEUE;
                      cache_line_next_valid_o   = 1'b1;
                      cache_line_next_dirty_o   = cache_line_dirty_i;
                      cache_line_broadcast_type = UPG;
                   end else begin
                      cache_line_next_state_o   = IM_AD;
                      cache_ctr_next_state_o    = ADD_TO_REQ_QUEUE;
                      cache_line_next_valid_o   = 1'b1;
                      cache_line_broadcast_type = GETM;
                   end // end else
                end
                I : begin
                   cache_line_next_state_o   = IM_AD;
                   cache_ctr_next_state_o    = ADD_TO_REQ_QUEUE;
                   cache_line_next_valid_o   = 1'b1;
                   cache_line_broadcast_type = GETM;
                end
                default:;
              endcase
           end
           // AMO : begin
           //    case (cache_line_state_i)
           //      I : begin
           //         cache_line_next_state_o   = IM_AD_AMO;
           //         cache_ctr_next_state_o    = ADD_TO_REQ_QUEUE;
           //         cache_line_next_valid_o   = 1'b1;
           //         cache_line_broadcast_type = GETM;
           //      end
           //      M_AMO : begin
           //         if(cache_line_tag_match_i) begin
           //            cache_line_next_state_o = M;
           //            cache_ctr_next_state_o  = ADD_TO_CACHE_RESP_QUEUE_STORE_AMO;
           //            cache_line_next_valid_o = 1'b1;
           //            cache_line_next_dirty_o = '1;
           //         end else begin
           //          $display("ERROR: Unexpected state");
           //         end
           //      end
           //      MI_AMO : begin
           //         if(cache_line_tag_match_i) begin
           //            cache_line_next_state_o = MI_A;
           //            cache_ctr_next_state_o  = ADD_TO_CACHE_RESP_QUEUE_STORE_AMO;
           //            cache_line_next_valid_o = 1'b1;
           //            cache_line_next_dirty_o = '1;
           //            cache_line_broadcast_type = PUTM;
           //         end else begin
           //          $display("ERROR: Unexpected state");
           //         end
           //      end
           //      default : /* default */;
           //    endcase
           // end
           OWN_GETM : begin
              case (cache_line_state_i)
                IM_AD : begin
                   cache_line_next_state_o = IM_D;
                   cache_ctr_next_state_o  = UPDATE_CACHE;
                   cache_line_next_valid_o = 1'b1;
                end
                IM_A : begin
                   cache_line_next_state_o = M;
                   cache_ctr_next_state_o  = UPDATE_CACHE;
                   cache_line_next_valid_o = 1'b1;
                end
                // IM_AD_AMO: begin
                //    cache_line_next_state_o = IM_D_AMO;
                //    cache_ctr_next_state_o  = UPDATE_CACHE;
                //    cache_line_next_valid_o = 1'b1;
                // end
                S : begin
                end
                I : begin
                   // cache_line_next_state_o = IS_AD;
                end
                default:;
              endcase
           end
           OWN_GETS : begin
              case (cache_line_state_i)
                IS_AD : begin
                   cache_line_next_state_o = IS_D;
                   cache_ctr_next_state_o  = UPDATE_CACHE;
                   cache_line_next_valid_o = 1'b1;
                end
                // IS_A : begin
                //    cache_line_next_state_o = S;
                //    cache_ctr_next_state_o  = UPDATE_CACHE;
                //    cache_line_next_valid_o = 1'b1;
                // end
                S : begin
                end
                I : begin
                   // cache_line_next_state_o = IS_AD;
                end
                default:;
              endcase
           end
           OTHER_GETM : begin
              case (cache_line_state_i)
                M : begin
                   if(cache_line_tag_match_i) begin
                      cache_line_next_state_o = MI_A; //write_back because other asking for the cache line
                      cache_ctr_next_state_o  = ADD_TO_WB_QUEUE;
                      cache_line_next_valid_o = 1'b1;
                      cache_line_next_dirty_o = cache_line_dirty_i;
                      cache_line_broadcast_type = PUTM;
                   end
                end
                S : begin
                   if(cache_line_tag_match_i) begin
                      cache_line_next_state_o = I;
                      cache_ctr_next_state_o  = UPDATE_CACHE;
                      cache_line_next_valid_o = 1'b0;
                      cache_line_next_dirty_o = 1'b0;
                   end
                end
                IM_D : begin
                   if(cache_line_tag_match_i) begin
                      cache_line_next_state_o = IM_DUI; //IM_DI;
                      cache_ctr_next_state_o  = UPDATE_CACHE;
                      cache_line_next_valid_o = 1'b1;
                      cache_line_next_dirty_o = 1'b0;
                   end
                end
                IS_D : begin
                   if(cache_line_tag_match_i) begin
                      cache_line_next_state_o = IS_DI;
                      cache_ctr_next_state_o  = UPDATE_CACHE;
                      cache_line_next_valid_o = 1'b1;
                      cache_line_next_dirty_o = 1'b0;
                   end
                end
                MS_A : begin
                  if(cache_line_tag_match_i) begin
                      cache_line_next_state_o = MI_A;
                      cache_ctr_next_state_o  = UPDATE_CACHE;
                      cache_line_next_valid_o = 1'b1;
                      cache_line_next_dirty_o = 1'b0;
                   end
                end
                SM_W: begin
                   if(cache_line_tag_match_i) begin
                      cache_line_next_state_o = IM_W;
                      cache_ctr_next_state_o  = UPDATE_CACHE;
                      cache_line_next_valid_o = 1'b1;
                      cache_line_next_dirty_o = 1'b0;
                   end
                end
                // IM_D_AMO : begin
                //    if(cache_line_tag_match_i) begin
                //       cache_line_next_state_o = IM_DI_AMO; //IM_DI;
                //       cache_ctr_next_state_o  = UPDATE_CACHE;
                //       cache_line_next_valid_o = 1'b1;
                //       cache_line_next_dirty_o = 1'b0;
                //    end
                // end
                // MI_AU : begin //MI_A //transitional state; not triggered from incomming events
                //    if(cache_line_tag_match_i) begin
                //       cache_line_next_state_o = MI_A;
                //       cache_ctr_next_state_o  = ADD_TO_WB_QUEUE;
                //       cache_line_next_valid_o = 1'b1;
                //       cache_line_next_dirty_o = 1'b1;
                //       cache_line_broadcast_type = PUTM;
                //    end
                // end
                I : begin
                end
                default:;
              endcase
           end
           OTHER_GETS : begin
              case (cache_line_state_i)
                M : begin
                   if(cache_line_tag_match_i) begin
                      cache_line_next_state_o = MS_A; //write_back because other asking for the cache line
                      cache_ctr_next_state_o  = ADD_TO_WB_QUEUE;
                      cache_line_next_valid_o = 1'b1;
                      cache_line_next_dirty_o = cache_line_dirty_i;
                      cache_line_broadcast_type = PUTM;
                   end
                end
                IM_D : begin
                   if(cache_line_tag_match_i) begin
                      cache_line_next_state_o = IM_DUS;
                      cache_ctr_next_state_o  = UPDATE_CACHE;
                      cache_line_next_valid_o = 1'b1;
                      cache_line_next_dirty_o = 1'b0;
                   end
                end
                // MS_AU : begin //MI_A //transitional state; not triggered from incomming events
                //    if(cache_line_tag_match_i) begin
                //       cache_line_next_state_o = MS_A;
                //       cache_ctr_next_state_o  = ADD_TO_WB_QUEUE;
                //       cache_line_next_valid_o = 1'b1;
                //       cache_line_next_dirty_o = 1'b1;
                //       cache_line_broadcast_type = PUTM;
                //    end
                // end
                default:;
              endcase
           end
           OWN_UPG    : begin
              case (cache_line_state_i)
                SM_W : begin
                  if(cache_line_tag_match_i) begin
                     cache_line_next_state_o = M;
                     cache_ctr_next_state_o  = ADD_TO_CACHE_RESP_QUEUE_STORE;
                     cache_line_next_valid_o = 1'b1;
                     cache_line_next_dirty_o = 1'b1;
                  end
                end
                IM_W: begin
                  if(cache_line_tag_match_i) begin
                    cache_line_next_state_o   = IM_AD;
                    cache_ctr_next_state_o    = ADD_TO_REQ_QUEUE;
                    cache_line_next_valid_o   = 1'b1;
                    cache_line_next_dirty_o   = cache_line_dirty_i;
                    cache_line_broadcast_type = GETM;
                  end
                end
                default:;
              endcase
           end
           OTHER_UPG : begin
              case (cache_line_state_i)
                S : begin
                  if(cache_line_tag_match_i) begin
                     cache_line_next_state_o = I;
                     cache_ctr_next_state_o  = UPDATE_CACHE;
                     cache_line_next_valid_o = 1'b0;
                     cache_line_next_dirty_o = 1'b0;
                  end 
                end
                SM_W: begin
                   if(cache_line_tag_match_i) begin
                      cache_line_next_state_o = IM_W;
                      cache_ctr_next_state_o  = UPDATE_CACHE;
                      cache_line_next_valid_o = 1'b1;
                      cache_line_next_dirty_o = 1'b0;
                   end
                end
                default:;
              endcase
           end
           OWN_PUTM : begin
              case (cache_line_state_i)
                MI_A: begin
                  if(cache_line_tag_match_i) begin
                     cache_line_next_state_o = I;
                     cache_ctr_next_state_o  = REMOVE_FROM_CACHE;
                     cache_line_next_valid_o = 1'b0;
                     cache_line_next_dirty_o = 1'b0;
                  end
                end 
                MS_A: begin 
                  if(cache_line_tag_match_i) begin
                   cache_line_next_state_o = S;
                   cache_ctr_next_state_o  = UPDATE_CACHE;
                   cache_line_next_valid_o = 1'b1;
                   cache_line_next_dirty_o = 1'b0;
                  end
                end
                // MI_AU: begin
                //    cache_line_next_state_o = I;
                //    cache_ctr_next_state_o  = REMOVE_FROM_CACHE_2;
                //    cache_line_next_valid_o = 1'b0;
                //    cache_line_next_dirty_o = 1'b0;
                // end 
                // MS_AU: begin
                //    cache_line_next_state_o = S;
                //    cache_ctr_next_state_o  = REMOVE_FROM_CACHE_2;
                //    cache_line_next_valid_o = 1'b1;
                //    cache_line_next_dirty_o = 1'b0;
                // end 
                default : /* default */;
              endcase
           end
           OTHER_PUTM : begin
           end
           // INV  : begin
           // end
           DATA : begin

              case (cache_line_state_i)
                IM_AD : begin
                  if(cache_line_tag_match_i) begin
                   cache_line_next_state_o = IM_A;
                   cache_ctr_next_state_o  = REFILL_CACHE; ///update cache state
                   cache_line_next_valid_o = 1'b1;
                  end

                end
                // IS_AD : begin
                //    cache_line_next_state_o = IS_A;
                //    cache_ctr_next_state_o  = REFILL_CACHE;
                //    cache_line_next_valid_o = 1'b1;
                // end
                IM_D : begin
                  if(cache_line_tag_match_i) begin
                   cache_line_next_state_o = M;
                   cache_ctr_next_state_o  = REFILL_CACHE;
                   cache_line_next_valid_o = 1'b1;
                  end

                end
                IS_D : begin
                  if(cache_line_tag_match_i) begin
                   cache_line_next_state_o = S;
                   cache_ctr_next_state_o  = REFILL_CACHE;
                   cache_line_next_valid_o = 1'b1;
                  end
                end
                MI_A : begin
                  if(cache_line_tag_match_i) begin
                   cache_line_next_state_o = I;
                   cache_ctr_next_state_o  = IDLE_2;
                   cache_line_next_valid_o = 1'b0;
                   cache_line_next_dirty_o = 1'b0;
                  end
                end
                IM_DUI : begin //IM_DI
                  if(cache_line_tag_match_i) begin
                   cache_line_next_state_o = IM_UI; //MI_AU; M_WI
                   cache_ctr_next_state_o  = REFILL_CACHE;
                   cache_line_next_valid_o = 1'b1;
                   cache_line_next_dirty_o = 1'b1;
                  end
                end
                IM_DUS : begin
                  if(cache_line_tag_match_i) begin
                   cache_line_next_state_o = IM_US;
                   cache_ctr_next_state_o  = REFILL_CACHE;
                   cache_line_next_valid_o = 1'b1;
                   cache_line_next_dirty_o = 1'b1;
                  end
                end
                IS_DI : begin
                  if(cache_line_tag_match_i) begin
                   cache_line_next_state_o = IS_UI;
                   cache_ctr_next_state_o  = REFILL_CACHE;
                   cache_line_next_valid_o = 1'b1;
                   cache_line_next_dirty_o = 1'b1;
                  end
                end
                // IM_D_AMO : begin
                //    cache_line_next_state_o = M_AMO;
                //    cache_ctr_next_state_o  = REFILL_CACHE;
                //    cache_line_next_valid_o = 1'b1;
                //    cache_line_next_dirty_o = 1'b1;
                // end
                // IM_DI_AMO : begin
                //    cache_line_next_state_o = MI_AMO;
                //    cache_ctr_next_state_o  = REFILL_CACHE;
                //    cache_line_next_valid_o = 1'b1;
                //    cache_line_next_dirty_o = 1'b1;
                // end
                I : begin
                   // cache_line_next_state_o = IS_AD;
                end
                default:;
              endcase
           end
           ACK : begin
              cache_line_next_state_o = I;
              cache_ctr_next_state_o  = LLCC_FLUSH_DONE;
              cache_line_next_valid_o = 1'b0;
              cache_line_next_dirty_o = 1'b0;
           end
           // NACK    : begin
           // end
           FLUSH : begin
            cache_line_next_state_o = I;
            cache_ctr_next_state_o  = CACHE_FLUSH_UPDATE;
            cache_line_next_valid_o = 1'b0;
            cache_line_next_dirty_o = 1'b0;
           end // FLUSH :
           LLCC_FLUSH : begin
            cache_line_next_state_o = I;
            cache_ctr_next_state_o  = ADD_FLUSH_TO_WB_QUEUE;
            cache_line_next_valid_o = 1'b0;
            cache_line_next_dirty_o = 1'b0;
           end // FLUSH :
           default:;
         endcase
      end // if(reset)
   end // always @(*)

endmodule // coherence_ctr
